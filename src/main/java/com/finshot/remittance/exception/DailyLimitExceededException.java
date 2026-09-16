package com.finshot.remittance.exception;

/**
 * Exception yang dilempar ketika total transaksi harian customer
 * melebihi batas maksimal 3.000.000 KRW (Part 2 - Limit Enforcement).
 *
 * Diterjemahkan oleh GlobalExceptionHandler menjadi HTTP 422 Unprocessable Entity
 * dengan error code "LIMIT_EXCEEDED".
 */
public class DailyLimitExceededException extends RuntimeException {

    public DailyLimitExceededException(String message) {
        super(message);
    }
}
