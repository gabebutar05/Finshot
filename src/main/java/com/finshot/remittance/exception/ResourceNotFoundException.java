package com.finshot.remittance.exception;

/**
 * Exception yang dilempar saat data transfer atau customer tidak ditemukan di database (Part 1.3).
 * Diterjemahkan oleh GlobalExceptionHandler menjadi HTTP 404 dengan error "NOT_FOUND".
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
