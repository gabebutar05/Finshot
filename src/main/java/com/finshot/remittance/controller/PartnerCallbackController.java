package com.finshot.remittance.controller;

import com.finshot.remittance.dto.PartnerCallbackRequest;
import com.finshot.remittance.dto.TransferResponse;
import com.finshot.remittance.partner.PartnerCallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller untuk menerima webhook HTTP POST callback dari mitra bank di Filipina (Part 2.3).
 *
 * Endpoint: POST /api/callbacks/partner
 *
 * Di C#/.NET: Setara dengan [ApiController] [Route("api/callbacks")] ControllerBase.
 */
@RestController
@RequestMapping("/api/callbacks")
public class PartnerCallbackController {

    private final PartnerCallbackService partnerCallbackService;

    public PartnerCallbackController(PartnerCallbackService partnerCallbackService) {
        this.partnerCallbackService = partnerCallbackService;
    }

    /**
     * Menerima notifikasi status akhir dari partner.
     *
     * Response:
     * - 200 OK dengan detail transfer berstatus COMPLETED
     * - 400 Bad Request jika parameter tidak valid
     * - 404 Not Found jika partnerRef tidak ditemukan
     * - 409 Conflict jika status transfer saat ini bukan SENDING
     */
    @PostMapping("/partner")
    public ResponseEntity<TransferResponse> handlePartnerCallback(@RequestBody PartnerCallbackRequest request) {
        TransferResponse response = partnerCallbackService.handleCallback(request);
        return ResponseEntity.ok(response);
    }
}
