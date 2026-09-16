package com.finshot.remittance.partner;

import com.finshot.remittance.dto.PartnerCallbackRequest;
import com.finshot.remittance.dto.TransferResponse;
import com.finshot.remittance.entity.PartnerCallbackEvent;
import com.finshot.remittance.entity.Transfer;
import com.finshot.remittance.entity.TransferStatus;
import com.finshot.remittance.exception.ConflictException;
import com.finshot.remittance.exception.ResourceNotFoundException;
import com.finshot.remittance.exception.ValidationException;
import com.finshot.remittance.repository.PartnerCallbackRepository;
import com.finshot.remittance.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service untuk memproses webhook callback dari partner secara idempoten (Part 2.3).
 *
 * Aturan Soal 2.3:
 * 1. Webhook menerima partnerRef, eventId, status ("COMPLETED").
 * 2. Idempoten berdasarkan eventId: jika eventId sudah pernah dicatat, jangan mutasikan state lagi,
 *    langsung kembalikan transfer saat ini.
 * 3. Validasi state: Callback hanya valid jika transaksi sedang berstatus SENDING.
 * 4. Transisi: Ubah status menjadi COMPLETED (satu-satunya cara transfer mencapai status COMPLETED).
 * 5. Catat riwayat callback di tabel partner_callbacks.
 */
@Service
public class PartnerCallbackService {

    private static final Logger log = LoggerFactory.getLogger(PartnerCallbackService.class);

    private final TransferRepository transferRepository;
    private final PartnerCallbackRepository partnerCallbackRepository;

    public PartnerCallbackService(TransferRepository transferRepository,
                                  PartnerCallbackRepository partnerCallbackRepository) {
        this.transferRepository = transferRepository;
        this.partnerCallbackRepository = partnerCallbackRepository;
    }

    @Transactional
    public TransferResponse handleCallback(PartnerCallbackRequest request) {
        if (request.getPartnerRef() == null || request.getPartnerRef().isBlank()) {
            throw new ValidationException("partnerRef is required");
        }
        if (request.getEventId() == null || request.getEventId().isBlank()) {
            throw new ValidationException("eventId is required");
        }
        if (!"COMPLETED".equalsIgnoreCase(request.getStatus())) {
            throw new ValidationException("Callback status must be COMPLETED");
        }

        // 1. Cek idempotensi eventId: jika eventId sudah pernah diproses, jangan ubah state lagi
        if (partnerCallbackRepository.existsById(request.getEventId())) {
            log.info("Callback eventId {} already processed. Returning current state idempotently.", request.getEventId());
            Transfer existingTransfer = transferRepository.findByPartnerRef(request.getPartnerRef())
                    .orElseThrow(() -> new ResourceNotFoundException("Transfer not found for partnerRef: " + request.getPartnerRef()));
            return TransferResponse.fromEntity(existingTransfer);
        }

        // 2. Cari transfer berdasarkan partnerRef
        Transfer transfer = transferRepository.findByPartnerRef(request.getPartnerRef())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown partnerRef: " + request.getPartnerRef()));

        // 3. Validasi status: Callback hanya sah jika transfer sedang dalam status SENDING
        if (transfer.getStatus() != TransferStatus.SENDING) {
            throw new ConflictException("Transfer is in status " + transfer.getStatus() + ". Callbacks are only valid from SENDING.");
        }

        // 4. Ubah status menjadi COMPLETED (satu-satunya cara transfer menjadi COMPLETED sesuai 2.3)
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setUpdatedAt(LocalDateTime.now());
        Transfer saved = transferRepository.save(transfer);

        // 5. Simpan catatan eventId callback untuk mencegah double-processing
        PartnerCallbackEvent eventRecord = new PartnerCallbackEvent(request.getEventId(), request.getPartnerRef(), LocalDateTime.now());
        partnerCallbackRepository.save(eventRecord);

        log.info("Transfer {} successfully COMPLETED via callback event {}", saved.getId(), request.getEventId());
        return TransferResponse.fromEntity(saved);
    }
}
