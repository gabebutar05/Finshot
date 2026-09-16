package com.finshot.remittance.repository;

import com.finshot.remittance.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data Access Layer untuk entitas Transfer (Part 1 & Part 2).
 *
 * Di C#/.NET: Setara dengan DbSet<Transfer> atau ITransferRepository.
 */
@Repository
public interface TransferRepository extends JpaRepository<Transfer, String> {

    /**
     * Mencari transfer berdasarkan Idempotency-Key (Part 1.3 / Q2).
     *
     * Digunakan untuk mengecek apakah request dengan idempotency key ini
     * sudah pernah diproses sebelumnya.
     */
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    /**
     * Mencari transfer berdasarkan partner reference (Part 2 - Callback).
     *
     * Digunakan ketika partner pengiriman uang mengirimkan webhook callback
     * dengan referensi transaksi mereka.
     */
    Optional<Transfer> findByPartnerRef(String partnerRef);

    /**
     * Menghitung total sendAmount (KRW) yang telah dikirim oleh customer pada rentang waktu tertentu
     * (misal: 1 hari kalender berjalan / Part 2 Limit Enforcement).
     *
     * Catatan Finansial:
     * Transaksi berstatus 'FAILED' dan 'CANCELLED' tidak dihitung ke dalam limit harian.
     * COALESCE digunakan agar jika belum ada transaksi, hasilnya adalah 0 (bukan null).
     */
    @Query("SELECT COALESCE(SUM(t.sendAmount), 0) FROM Transfer t " +
            "WHERE t.customerId = :customerId " +
            "AND t.createdAt >= :startOfDay AND t.createdAt < :endOfDay " +
            "AND t.status NOT IN ('FAILED', 'CANCELLED')")
    long sumSendAmountByCustomerAndDate(
            @Param("customerId") String customerId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
