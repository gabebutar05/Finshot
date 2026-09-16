package com.finshot.remittance.dto;

/**
 * Data Transfer Object (DTO) untuk menangkap body JSON dari request pembuatan transfer (Part 1.3).
 *
 * Contoh request JSON dari soal Finshot:
 * {
 *   "customerId": "C001",
 *   "sendCurrency": "KRW",
 *   "sendAmount": "500000",
 *   "receiveCurrency": "PHP",
 *   "recipientName": "Juan Dela Cruz"
 * }
 */
public class CreateTransferRequest {

    /**
     * ID nasabah pengirim (misal: "C001").
     */
    private String customerId;

    /**
     * Mata uang pengiriman (harus "KRW").
     */
    private String sendCurrency;

    /**
     * Nominal uang kirim dalam bentuk String persis sesuai format soal ("500000").
     * Menggunakan String di DTO memudahkan kita memvalidasi apakah input mengandung
     * karakter non-angka, angka negatif, atau pecahan desimal sebelum di-parse ke Long.
     */
    private String sendAmount;

    /**
     * Mata uang penerima (harus "PHP").
     */
    private String receiveCurrency;

    /**
     * Nama penerima di Filipina (1 - 100 karakter).
     */
    private String recipientName;

    /**
     * Opsional: partnerRef untuk keperluan pengujian khusus (misal pengujian S9).
     * Jika tidak dikirim oleh klien, sistem akan membuatkan otomatis.
     */
    private String partnerRef;

    // No-arg constructor wajib untuk Jackson JSON deserializer
    public CreateTransferRequest() {
    }

    public CreateTransferRequest(String customerId, String sendCurrency, String sendAmount, String receiveCurrency, String recipientName) {
        this.customerId = customerId;
        this.sendCurrency = sendCurrency;
        this.sendAmount = sendAmount;
        this.receiveCurrency = receiveCurrency;
        this.recipientName = recipientName;
    }

    // ==========================================
    // Getter dan Setter
    // ==========================================

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getSendCurrency() {
        return sendCurrency;
    }

    public void setSendCurrency(String sendCurrency) {
        this.sendCurrency = sendCurrency;
    }

    public String getSendAmount() {
        return sendAmount;
    }

    public void setSendAmount(String sendAmount) {
        this.sendAmount = sendAmount;
    }

    public String getReceiveCurrency() {
        return receiveCurrency;
    }

    public void setReceiveCurrency(String receiveCurrency) {
        this.receiveCurrency = receiveCurrency;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPartnerRef() {
        return partnerRef;
    }

    public void setPartnerRef(String partnerRef) {
        this.partnerRef = partnerRef;
    }
}
