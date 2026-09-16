package com.finshot.remittance.dto;

/**
 * Data Transfer Object (DTO) untuk menangkap body JSON dari webhook callback partner Filipina (Part 2.3).
 *
 * Contoh payload:
 * {
 *   "partnerRef": "REF-123456",
 *   "eventId": "evt_001",
 *   "status": "COMPLETED"
 * }
 */
public class PartnerCallbackRequest {

    private String partnerRef;
    private String eventId;
    private String status;

    public PartnerCallbackRequest() {
    }

    public PartnerCallbackRequest(String partnerRef, String eventId, String status) {
        this.partnerRef = partnerRef;
        this.eventId = eventId;
        this.status = status;
    }

    public String getPartnerRef() {
        return partnerRef;
    }

    public void setPartnerRef(String partnerRef) {
        this.partnerRef = partnerRef;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
