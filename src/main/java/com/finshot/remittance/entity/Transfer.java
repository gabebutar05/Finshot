package com.finshot.remittance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "send_currency", nullable = false)
    private String sendCurrency;

    @Column(name = "send_amount", nullable = false)
    private Long sendAmount;

    @Column(nullable = false)
    private Long fee;

    @Column(name = "total_debit", nullable = false)
    private Long totalDebit;

    @Column(name = "receive_currency", nullable = false)
    private String receiveCurrency;

    @Column(name = "receive_amount", nullable = false)
    private BigDecimal receiveAmount;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getter & Setter
}