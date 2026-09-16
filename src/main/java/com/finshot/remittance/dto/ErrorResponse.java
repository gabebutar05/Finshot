package com.finshot.remittance.dto;

/**
 * Data Transfer Object (DTO) untuk format respons error non-2xx (Part 1.3).
 *
 * Soal Finshot secara eksplisit melarang membuat model error buatan sendiri:
 * "Every non-2xx response uses this shape. Do not design your own error model."
 *
 * Format baku dari soal:
 * {
 *   "error": "VALIDATION_ERROR",
 *   "message": "sendAmount must be at least 10000"
 * }
 *
 * Nilai "error":
 * - VALIDATION_ERROR (HTTP 400)
 * - NOT_FOUND        (HTTP 404)
 * - CONFLICT         (HTTP 409)
 * - LIMIT_EXCEEDED   (HTTP 422 - Part 2)
 */
public class ErrorResponse {

    /**
     * Kode error baku yang dikenali sistem (contoh: "VALIDATION_ERROR", "CONFLICT").
     */
    private String error;

    /**
     * Pesan penjelasan yang mudah dibaca oleh manusia.
     */
    private String message;

    // No-arg constructor wajib untuk Jackson serializer
    public ErrorResponse() {
    }

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    // ==========================================
    // Getter dan Setter
    // ==========================================

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
