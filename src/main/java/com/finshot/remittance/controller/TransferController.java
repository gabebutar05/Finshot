package com.finshot.remittance.controller;

import com.finshot.remittance.dto.CreateTransferRequest;
import com.finshot.remittance.entity.Transfer;
import com.finshot.remittance.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<Transfer> createTransfer(
            @RequestBody CreateTransferRequest request) {

        Transfer transfer = transferService.createTransfer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transfer);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Finshot Remittance API is running";
    }
}