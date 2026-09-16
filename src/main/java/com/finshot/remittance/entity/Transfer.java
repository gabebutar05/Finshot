package com.finshot.remittance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entitas JPA utama yang memetakan tabel `transfers`.
 * Menyimpan siklus hidup transaksi, nilai finansial presisi, dan token idempotensi.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

    /**
     * ID transaksi (UUID v4 string 36 karakter).
     */
    @Id
    @Column(name = "id", length = 36)
    private String id;

    /**
     * Foreign Key merujuk ke customers.customer_id.
     */
    @Column(name = "customer_id", nullable = false, length = 10)
    private String customerId;

    /**
     * Token idempotensi unik yang dikirim oleh klien via header Idempotency-Key.
     */
    @Column(name = "idempotency_key", nullable = false, length = 100, unique = true)
    private String idempotencyKey;

    /**
     * Sidik jari SHA-256 dari isi request body untuk mendeteksi perubahan payload pada retry.
     */
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    /**
     * Nomor referensi transaksi untuk mitra partner di Filipina.
     */
    @Column(name = "partner_ref", length = 100)
    private String partnerRef;

    /**
     * Mata uang pengirim: KRW (Won Korea).
     */
    @Column(name = "send_currency", nullable = false, length = 3)
    private String sendCurrency;

    /**
     * Nominal uang kirim (KRW).
     * Menggunakan Long (BIGINT) karena KRW tidak memiliki pecahan desimal/sen.
     */
    @Column(name = "send_amount", nullable = false)
    private Long sendAmount;

    /**
     * Biaya transfer (Fee KRW): 1% dari sendAmount, minimal 3,000 KRW, dibulatkan HALF_UP.
     */
    @Column(name = "fee", nullable = false)
    private Long fee;

    /**
     * Total debit nasabah: sendAmount + fee.
     */
    @Column(name = "total_debit", nullable = false)
    private Long totalDebit;

    /**
     * Mata uang penerima: PHP (Philippine Peso).
     */
    @Column(name = "receive_currency", nullable = false, length = 3)
    private String receiveCurrency;

    /**
     * Nominal yang diterima di Filipina (PHP): sendAmount * 0.0412.
     * Menggunakan BigDecimal (DECIMAL(18,2)) karena PHP memiliki 2 digit sen di belakang koma.
     */
    @Column(name = "receive_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal receiveAmount;

    /**
     * Nama penerima di Filipina (maksimal 100 karakter).
     */
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    /**
     * Status transaksi saat ini (REQUESTED, SENDING, COMPLETED, FAILED, CANCELLED).
     * Disimpan sebagai String di database.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    /**
     * Alasan kegagalan jika transaksi menjadi FAILED.
     */
    @Column(name = "fail_reason", length = 255)
    private String failReason;

    /**
     * Waktu pencatatan transaksi dibuat.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Waktu pembaruan terakhir transaksi.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Transfer() {
    }

    // ==========================================
    // Getter dan Setter
    // ==========================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public String getPartnerRef() {
        return partnerRef;
    }

    public void setPartnerRef(String partnerRef) {
        this.partnerRef = partnerRef;
    }

    public String getSendCurrency() {
        return sendCurrency;
    }

    public void setSendCurrency(String sendCurrency) {
        this.sendCurrency = sendCurrency;
    }

    public Long getSendAmount() {
        return sendAmount;
    }

    public void setSendAmount(Long sendAmount) {
        this.sendAmount = sendAmount;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public Long getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(Long totalDebit) {
        this.totalDebit = totalDebit;
    }

    public String getReceiveCurrency() {
        return receiveCurrency;
    }

    public void setReceiveCurrency(String receiveCurrency) {
        this.receiveCurrency = receiveCurrency;
    }

    public BigDecimal getReceiveAmount() {
        return receiveAmount;
    }

    public void setReceiveAmount(BigDecimal receiveAmount) {
        this.receiveAmount = receiveAmount;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
