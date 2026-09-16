package com.finshot.remittance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entitas JPA yang merepresentasikan tabel `partner_callbacks`.
 * Digunakan untuk mencatat riwayat eventId callback dari mitra Filipina
 * agar proses webhook bersifat idempoten (tidak dieksekusi ganda).
 */
@Entity
@Table(name = "partner_callbacks")
public class PartnerCallbackEvent {

    /**
     * event_id: ID unik untuk setiap peristiwa/kejadian callback (Primary Key).
     * Contoh: "evt_0001".
     */
    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    /**
     * partner_ref: Referensi transfer milik partner yang bersangkutan.
     */
    @Column(name = "partner_ref", nullable = false, length = 100)
    private String partnerRef;

    /**
     * Waktu saat callback ini pertama kali diproses oleh sistem kita.
     */
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    // No-arg constructor wajib untuk Hibernate/JPA
    public PartnerCallbackEvent() {
    }

    public PartnerCallbackEvent(String eventId, String partnerRef, LocalDateTime processedAt) {
        this.eventId = eventId;
        this.partnerRef = partnerRef;
        this.processedAt = processedAt;
    }

    // ==========================================
    // Getter dan Setter
    // ==========================================

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPartnerRef() {
        return partnerRef;
    }

    public void setPartnerRef(String partnerRef) {
        this.partnerRef = partnerRef;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
