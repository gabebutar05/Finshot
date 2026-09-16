package com.finshot.remittance.exception;

/**
 * Exception yang dilempar saat terjadi kegagalan validasi input data (Part 1.3).
 * Diterjemahkan oleh GlobalExceptionHandler menjadi HTTP 400 dengan error "VALIDATION_ERROR".
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
