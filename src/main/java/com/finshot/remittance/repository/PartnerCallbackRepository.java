package com.finshot.remittance.repository;

import com.finshot.remittance.entity.PartnerCallbackEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data Access Layer untuk mencatat riwayat webhook callback dari partner (Part 2).
 *
 * Digunakan untuk menjamin idempotensi webhook:
 * Jika partner mengirimkan callback dengan event_id yang sama berulang kali (karena network retry),
 * sistem tidak akan memproses mutasi status berulang kali.
 */
@Repository
public interface PartnerCallbackRepository extends JpaRepository<PartnerCallbackEvent, String> {

    /**
     * Mencari riwayat callback berdasarkan eventId unik dari partner.
     */
    Optional<PartnerCallbackEvent> findByEventId(String eventId);

    /**
     * Mengecek keberadaan eventId unik dari partner secara efisien.
     */
    boolean existsByEventId(String eventId);
}
