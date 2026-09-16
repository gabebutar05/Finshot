package com.finshot.remittance.entity;

/**
 * Enum untuk status siklus hidup transaksi pengiriman uang (State Machine - Part 1.2).
 *
 * Alur transisi yang sah:
 * REQUESTED ──► SENDING ──► COMPLETED
 *     │             │
 *     │             └──────► FAILED
 *     │
 *     └──► CANCELLED
 *
 * Transisi selain alur di atas akan menghasilkan HTTP 409 CONFLICT.
 */
public enum TransferStatus {
    /**
     * REQUESTED: Status awal saat transaksi baru dibuat oleh nasabah.
     * Pada status ini, transaksi masih bisa dibatalkan (CANCELLED).
     */
    REQUESTED,

    /**
     * SENDING: Transaksi sedang dalam proses pengiriman instruksi ke mitra di Filipina.
     */
    SENDING,

    /**
     * COMPLETED: Dana berhasil diterima oleh penerima di Filipina.
     * Hanya bisa dicapai melalui konfirmasi resmi dari callback mitra (Part 2.3).
     */
    COMPLETED,

    /**
     * FAILED: Pengiriman ke mitra gagal setelah dilakukan 3 kali percobaan (retry) berturut-turut.
     */
    FAILED,

    /**
     * CANCELLED: Transaksi dibatalkan oleh nasabah.
     * Hanya diizinkan jika status masih REQUESTED.
     */
    CANCELLED
}
