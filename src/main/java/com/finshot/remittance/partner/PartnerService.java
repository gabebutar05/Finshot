package com.finshot.remittance.partner;

import com.finshot.remittance.entity.Transfer;
import com.finshot.remittance.entity.TransferStatus;
import com.finshot.remittance.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service untuk memproses pengiriman ke partner secara asinkron dengan mekanisme retry dan backoff (Part 2.1).
 *
 * Aturan Soal 2.1:
 * 1. Panggil partnerClient.call(partnerRef) maksimal 3 kali (Retry 3x).
 * 2. Gunakan jeda backoff antar percobaan (misal 100ms, 200ms).
 * 3. Jika partner menerima (call return true): status tetap SENDING menunggu webhook callback.
 * 4. Jika seluruh 3 percobaan gagal: status berubah menjadi FAILED dengan catatan failReason.
 *
 * Di C#/.NET: Setara dengan BackgroundService / IHostedService / Hangfire Job dengan Polly Retry Policy.
 */
@Service
public class PartnerService {

    private static final Logger log = LoggerFactory.getLogger(PartnerService.class);

    private final PartnerClient partnerClient;
    private final TransferRepository transferRepository;

    public PartnerService(PartnerClient partnerClient, TransferRepository transferRepository) {
        this.partnerClient = partnerClient;
        this.transferRepository = transferRepository;
    }

    /**
     * Memproses pengiriman transfer ke partner di thread latar belakang (Asinkron).
     * Diberikan jeda awal 1 detik agar client/user memiliki kesempatan membatalkan (cancel)
     * saat status masih REQUESTED.
     */
    @Async
    public void initiatePartnerSendingAsync(String transferId) {
        try {
            // Jeda 1 detik agar pengujian cancel cepat (S4) tidak terhalang race condition
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        processPartnerSending(transferId);
    }

    /**
     * Logika inti pengiriman ke partner dengan retry loop 3 kali.
     * Dibuat public agar dapat diuji secara langsung (sinkron) di unit/integration test.
     */
    @Transactional
    public void processPartnerSending(String transferId) {
        Transfer transfer = transferRepository.findById(transferId).orElse(null);
        if (transfer == null) {
            log.warn("Transfer {} not found for partner processing", transferId);
            return;
        }

        // Jika transaksi sudah dibatalkan pengguna saat berstatus REQUESTED, jangan kirim ke partner
        if (transfer.getStatus() == TransferStatus.CANCELLED) {
            log.info("Transfer {} is already CANCELLED. Aborting partner sending.", transferId);
            return;
        }

        // Transisi status: REQUESTED -> SENDING
        if (transfer.getStatus() == TransferStatus.REQUESTED) {
            transfer.setStatus(TransferStatus.SENDING);
            transfer.setUpdatedAt(LocalDateTime.now());
            transfer = transferRepository.save(transfer);
            log.info("Transfer {} transitioned to SENDING", transfer.getId());
        }

        String partnerRef = transfer.getPartnerRef();
        int maxAttempts = 3;
        boolean accepted = false;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean success = partnerClient.call(partnerRef);
            if (success) {
                log.info("[Partner Attempt {}/{}] partnerRef: {} - SUCCESS (Accepted)",
                        attempt, maxAttempts, partnerRef);
                accepted = true;
                break;
            } else {
                log.warn("[Partner Attempt {}/{}] partnerRef: {} - FAILED (Rejected)",
                        attempt, maxAttempts, partnerRef);
                if (attempt < maxAttempts) {
                    try {
                        // Exponential backoff: percobaan ke-1 tunggu 100ms, ke-2 tunggu 200ms
                        Thread.sleep(100L * attempt);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (accepted) {
            // Sesuai 2.1: Jika partner menerima, status tetap SENDING menunggu webhook callback
            log.info("Partner accepted transfer {}. Stays in SENDING awaiting callback.", transfer.getId());
        } else {
            // Sesuai 2.1: Jika 3x gagal, status berubah jadi FAILED beserta alasannya
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setFailReason("Partner rejected after 3 attempts");
            transfer.setUpdatedAt(LocalDateTime.now());
            transferRepository.save(transfer);
            log.warn("All 3 attempts failed. Transfer {} transitioned to FAILED.", transfer.getId());
        }
    }
}
