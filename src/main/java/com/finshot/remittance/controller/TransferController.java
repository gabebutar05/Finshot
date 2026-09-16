package com.finshot.remittance.controller;

import com.finshot.remittance.dto.CreateTransferRequest;
import com.finshot.remittance.dto.TransferResponse;
import com.finshot.remittance.exception.ValidationException;
import com.finshot.remittance.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST API untuk mengelola transaksi remitansi (Part 1.3).
 *
 * Di C#/.NET: Setara dengan [ApiController] [Route("api/transfers")] ControllerBase.
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Membuat transaksi transfer baru (Part 1.3 Create Transfer).
     *
     * Endpoint: POST /api/transfers
     * Header Wajib: Idempotency-Key
     *
     * Response:
     * - 201 Created jika transaksi berhasil dibuat (atau di-replay secara idempoten)
     * - 400 Bad Request jika input data atau header tidak valid
     * - 409 Conflict jika Idempotency-Key dipakai ulang dengan data payload berbeda
     * - 422 Unprocessable Entity jika melebihi batas limit harian 5.000.000 KRW
     */
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateTransferRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("Missing required header: Idempotency-Key");
        }

        TransferResponse response = transferService.createTransfer(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Mengambil detail transaksi transfer (Part 1.3 Read Transfer).
     *
     * Endpoint: GET /api/transfers/{id}
     *
     * Response:
     * - 200 OK jika ditemukan
     * - 404 Not Found jika ID transfer tidak ada di database
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable("id") String id) {
        TransferResponse response = transferService.getTransfer(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Membatalkan transaksi transfer (Part 1.3 Cancel Transfer).
     *
     * Endpoint: POST /api/transfers/{id}/cancel
     *
     * Aturan State Machine (Part 1.2):
     * - Hanya transfer dengan status REQUESTED yang dapat dibatalkan (200 OK -> status CANCELLED).
     * - Jika status selain REQUESTED (misal SENDING, COMPLETED), kembalikan 409 Conflict.
     * - Jika transfer tidak ditemukan, kembalikan 404 Not Found.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransferResponse> cancelTransfer(@PathVariable("id") String id) {
        TransferResponse response = transferService.cancelTransfer(id);
        return ResponseEntity.ok(response);
    }
}
