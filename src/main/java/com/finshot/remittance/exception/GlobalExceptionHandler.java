package com.finshot.remittance.exception;

import com.finshot.remittance.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler (Part 1.3).
 *
 * Menangkap semua exception yang dilempar dari layer Controller & Service,
 * lalu mengubahnya menjadi format JSON ErrorResponse standar:
 * {
 *   "error": "ERROR_CODE",
 *   "message": "Deskripsi error"
 * }
 *
 * Padanan di C#/.NET: Global Exception Middleware (UseExceptionHandler / Custom Middleware).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Menangani kegagalan validasi input bisnis (HTTP 400).
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }

    /**
     * Menangani header HTTP yang wajib namun tidak dikirim oleh client (misal: Idempotency-Key).
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "Missing required header: " + ex.getHeaderName()));
    }

    /**
     * Menangani payload request JSON yang rusak atau tidak bisa di-parse.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "Malformed JSON request body: " + ex.getMostSpecificCause().getMessage()));
    }

    /**
     * Menangani resource yang tidak ditemukan di database (HTTP 404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    /**
     * Menangani konflik idempotency atau state machine transfer (HTTP 409).
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", ex.getMessage()));
    }

    /**
     * Menangani batas limit harian 5.000.000 KRW yang terlampaui (HTTP 422 - Part 2).
     */
    @ExceptionHandler(DailyLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDailyLimit(DailyLimitExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.valueOf(422))
                .body(new ErrorResponse("LIMIT_EXCEEDED", ex.getMessage()));
    }

    /**
     * Fallback untuk error yang tidak terduga di server (HTTP 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", ex.getMessage()));
    }
}
