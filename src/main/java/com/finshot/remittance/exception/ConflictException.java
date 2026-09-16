package com.finshot.remittance.exception;

/**
 * Exception yang dilempar saat terjadi konflik transaksi (Part 1.2 & 1.3):
 * 1. Idempotency-Key yang sama digunakan kembali dengan data request yang berbeda.
 * 2. Mencoba membatalkan transaksi yang statusnya bukan REQUESTED.
 *
 * Diterjemahkan oleh GlobalExceptionHandler menjadi HTTP 409 dengan error "CONFLICT".
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
