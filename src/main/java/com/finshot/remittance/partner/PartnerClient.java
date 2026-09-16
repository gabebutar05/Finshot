package com.finshot.remittance.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simulasi Partner Client deterministik (Part 2.1).
 *
 * Mengembalikan hasil gagal atau sukses murni secara deterministik berdasarkan partnerRef
 * tanpa menggunakan random generator (sehingga hasil tes dapat direproduksi secara konsisten).
 *
 * Syarat Soal 2.1:
 * 1. Setiap pemanggilan memakan waktu minimal 200 ms (simulasi latensi jaringan/bank).
 * 2. Gagal jika Math.abs(partnerRef.hashCode()) % 10 < 3 ATAU digit terakhir adalah '0', '1', atau '2'.
 */
@Component
public class PartnerClient {

    private static final Logger log = LoggerFactory.getLogger(PartnerClient.class);

    /**
     * Memanggil API partner transfer.
     *
     * @param partnerRef kode referensi transaksi partner
     * @return true jika transaksi berhasil diterima oleh partner, false jika ditolak/timeout
     */
    public boolean call(String partnerRef) {
        try {
            // Simulasi latensi jaringan minimal 200 ms sesuai spesifikasi
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (partnerRef == null || partnerRef.isEmpty()) {
            return false;
        }

        // Aturan deterministik dari spesifikasi Finshot:
        // 1. Gagal jika remainder hashCode bernilai 0, 1, atau 2 (< 3)
        // 2. Gagal jika karakter terakhir adalah '0', '1', atau '2' (memudahkan pengujian manual/otomatis)
        int hashRemainder = Math.abs(partnerRef.hashCode()) % 10;
        char lastChar = partnerRef.charAt(partnerRef.length() - 1);
        boolean endsWithFailDigit = (lastChar == '0' || lastChar == '1' || lastChar == '2');

        boolean fails = (hashRemainder < 3) || endsWithFailDigit;

        log.info("PartnerClient dispatch: partnerRef={}, success={}", partnerRef, !fails);
        return !fails;
    }
}
