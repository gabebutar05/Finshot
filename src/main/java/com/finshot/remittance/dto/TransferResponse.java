package com.finshot.remittance.dto;

import com.finshot.remittance.entity.Transfer;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Data Transfer Object (DTO) untuk format respons transaksi transfer (Part 1.3).
 *
 * Contoh response 201 Created dari soal Finshot:
 * {
 *   "transferId": "...",
 *   "status": "REQUESTED",
 *   "sendAmount": "500000",
 *   "fee": "5000",
 *   "totalDebit": "505000",
 *   "receiveAmount": "20600.00",
 *   "createdAt": "..."
 * }
 */
public class TransferResponse {

    private String transferId;
    private String status;
    private String sendAmount;
    private String fee;
    private String totalDebit;
    private String receiveAmount;
    private String createdAt;
    private String partnerRef;
    private String failReason;

    // No-arg constructor wajib untuk serialisasi/deserialisasi
    public TransferResponse() {
    }

    /**
     * Factory method: Mengonversi objek internal Entity `Transfer`
     * menjadi DTO `TransferResponse` yang siap dikirim sebagai JSON ke klien.
     */
    public static TransferResponse fromEntity(Transfer transfer) {
        TransferResponse response = new TransferResponse();

        // Nama kolom di database adalah 'id', tetapi di response JSON soal wajib 'transferId'
        response.setTransferId(transfer.getId());
        response.setStatus(transfer.getStatus().name());

        // Angka finansial dikonversi ke format String sesuai format contoh response Finshot
        response.setSendAmount(String.valueOf(transfer.getSendAmount()));
        response.setFee(String.valueOf(transfer.getFee()));
        response.setTotalDebit(String.valueOf(transfer.getTotalDebit()));

        // Syarat Soal 1.1: receiveAmount HARUS selalu 2 digit desimal termasuk trailing zeros (contoh: "20600.00").
        // Locale.US menjamin tanda desimal selalu titik (.) bukan koma (,).
        response.setReceiveAmount(String.format(Locale.US, "%.2f", transfer.getReceiveAmount()));

        // Format tanggal ISO (contoh: "2026-09-16T12:00:00")
        response.setCreatedAt(transfer.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        response.setPartnerRef(transfer.getPartnerRef());
        response.setFailReason(transfer.getFailReason());

        return response;
    }

    // ==========================================
    // Getter dan Setter
    // ==========================================

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSendAmount() {
        return sendAmount;
    }

    public void setSendAmount(String sendAmount) {
        this.sendAmount = sendAmount;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public String getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(String totalDebit) {
        this.totalDebit = totalDebit;
    }

    public String getReceiveAmount() {
        return receiveAmount;
    }

    public void setReceiveAmount(String receiveAmount) {
        this.receiveAmount = receiveAmount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPartnerRef() {
        return partnerRef;
    }

    public void setPartnerRef(String partnerRef) {
        this.partnerRef = partnerRef;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}
