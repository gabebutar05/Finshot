package com.finshot.remittance;

import com.finshot.remittance.entity.Transfer;
import com.finshot.remittance.entity.TransferStatus;
import com.finshot.remittance.partner.PartnerService;
import com.finshot.remittance.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Automated Integration Test Suite Lengkap untuk Finshot Remittance API (Part 1 & Part 2).
 *
 * Menguji secara komprehensif seluruh skenario dari S1 hingga S9:
 * - S1: Pembuatan Transfer Sukses (Happy Path) & Read Detail
 * - S2: Aturan Minimum Fee 3.000 KRW (10.000 KRW)
 * - S3: Idempotensi (Replay Sukses & Deteksi Konflik HTTP 409)
 * - S4: State Machine (Pembatalan Status REQUESTED -> CANCELLED)
 * - S5: Validasi Penolakan Pembatalan Ulang (HTTP 409)
 * - S6: Validasi Input (Nominal < 10.000, Mata Uang Salah, dsb.)
 * - S7: Limit Harian Akumulasi 5.000.000 KRW (HTTP 422 LIMIT_EXCEEDED)
 * - S8: Webhook Partner Callback Idempoten (Status COMPLETED via eventId)
 * - S9: Kegagalan Deterministik Partner & Mekanisme Retry 3x (Status FAILED)
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TransferScenariosTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private PartnerService partnerService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String extractTransferId(String responseJson) {
        Pattern pattern = Pattern.compile("\"transferId\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(responseJson);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("transferId not found in response: " + responseJson);
    }

    // =========================================================================
    // Skenario S1: Happy Path Create Transfer & Read
    // =========================================================================
    @Test
    @DisplayName("S1: Pembuatan transfer sukses (500.000 KRW -> 20.600 PHP) dan status awal REQUESTED")
    void testScenario1_happyPath() throws Exception {
        String s1Payload = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "500000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Juan Dela Cruz"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "ik-test-s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s1Payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sendAmount").value("500000"))
                .andExpect(jsonPath("$.fee").value("5000"))             // 1% dari 500.000 = 5.000
                .andExpect(jsonPath("$.totalDebit").value("505000"))     // 500.000 + 5.000
                .andExpect(jsonPath("$.receiveAmount").value("20600.00"))// 500.000 * 0.0412
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn();

        String transferId = extractTransferId(result.getResponse().getContentAsString());
        assertNotNull(transferId);

        // Verifikasi detail via GET
        mockMvc.perform(get("/api/transfers/" + transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(transferId))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    // =========================================================================
    // Skenario S2: Minimum Fee Enforcement
    // =========================================================================
    @Test
    @DisplayName("S2: Nominal kecil (10.000 KRW) harus dikenakan fee minimum 3.000 KRW")
    void testScenario2_minimumFee() throws Exception {
        String s2Payload = """
                {
                  "customerId": "C002",
                  "sendCurrency": "KRW",
                  "sendAmount": "10000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Maria Santos"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "ik-test-s2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s2Payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sendAmount").value("10000"))
                .andExpect(jsonPath("$.fee").value("3000"))             // Min fee terpasang!
                .andExpect(jsonPath("$.totalDebit").value("13000"))     // 10.000 + 3.000
                .andExpect(jsonPath("$.receiveAmount").value("412.00")); // 10.000 * 0.0412
    }

    // =========================================================================
    // Skenario S3: Idempotency Replay & Conflict Detection
    // =========================================================================
    @Test
    @DisplayName("S3: Idempotency key sama + payload sama -> return data lama; payload beda -> HTTP 409")
    void testScenario3_idempotency() throws Exception {
        String originalPayload = """
                {
                  "customerId": "C003",
                  "sendCurrency": "KRW",
                  "sendAmount": "100000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Pedro Penduko"
                }
                """;
        String idempotencyKey = "ik-test-s3";

        // Step 1: Request awal
        MvcResult res1 = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String id1 = extractTransferId(res1.getResponse().getContentAsString());

        // Step 2: Kirim ulang dengan data sama (Idempotency Replay)
        MvcResult res2 = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String id2 = extractTransferId(res2.getResponse().getContentAsString());
        assertEquals(id1, id2, "ID harus sama (tidak boleh menduplikasi transaksi)");

        // Step 3: Kirim ulang dengan data berbeda (sendAmount diubah) -> 409 CONFLICT
        String modifiedPayload = """
                {
                  "customerId": "C003",
                  "sendCurrency": "KRW",
                  "sendAmount": "200000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Pedro Penduko"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modifiedPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // =========================================================================
    // Skenario S4 & S5: State Machine Pembatalan Transfer
    // =========================================================================
    @Test
    @DisplayName("S4 & S5: Status REQUESTED berhasil dibatalkan, namun pembatalan ulang ditolak HTTP 409")
    void testScenario4And5_cancelTransfer() throws Exception {
        String s4Payload = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "50000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Cardo Dalisay"
                }
                """;
        String idempotencyKey = "ik-test-s4-s5";

        MvcResult createResult = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s4Payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn();

        String transferId = extractTransferId(createResult.getResponse().getContentAsString());

        // S4: Batalkan transfer
        mockMvc.perform(post("/api/transfers/" + transferId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(transferId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // S5: Batalkan transaksi yang sama sekali lagi -> harus 409 CONFLICT
        mockMvc.perform(post("/api/transfers/" + transferId + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // =========================================================================
    // Skenario S6: Validasi Input Gagal (HTTP 400)
    // =========================================================================
    @Test
    @DisplayName("S6: Validasi Nilai SendAmount di bawah batas minimal (5,000 KRW < 10,000 KRW)")
    void testScenario6_validation() throws Exception {
        String s6Payload = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "5000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Juan Dela Cruz"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "ik-test-s6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s6Payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("sendAmount must be at least 10000"));
    }

    // =========================================================================
    // Skenario S7: Daily Limit 3.000.000 KRW
    // =========================================================================
    @Test
    @DisplayName("S7: Daily Limit 3.000.000 KRW untuk Customer C002 menghasilkan HTTP 422 LIMIT_EXCEEDED")
    void testScenario7_dailyLimit() throws Exception {
        // Kirim 2.000.000 KRW (Sukses)
        String req1 = """
                {
                  "customerId": "C002",
                  "sendCurrency": "KRW",
                  "sendAmount": "2000000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Recipient 1"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "ik-limit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req1))
                .andExpect(status().isCreated());

        // Kirim 1.000.000 KRW lagi (Total akumulasi 3.000.000 KRW -> Pas batas limit -> Sukses)
        String req2 = """
                {
                  "customerId": "C002",
                  "sendCurrency": "KRW",
                  "sendAmount": "1000000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Recipient 2"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "ik-limit-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req2))
                .andExpect(status().isCreated());

        // Kirim 10.000 KRW tambahan -> Melebihi limit harian 3.000.000 KRW -> 422 LIMIT_EXCEEDED
        String req3 = """
                {
                  "customerId": "C002",
                  "sendCurrency": "KRW",
                  "sendAmount": "10000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Recipient 3"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "ik-limit-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req3))
                .andExpect(status().isUnprocessableEntity()) // 422
                .andExpect(jsonPath("$.error").value("LIMIT_EXCEEDED"));
    }

    // =========================================================================
    // Skenario S8: Webhook Partner Callback Idempoten (COMPLETED)
    // =========================================================================
    @Test
    @DisplayName("S8: Callback Partner Idempoten berdasarkan eventId mengubah status menjadi COMPLETED")
    void testScenario8_partnerCallback() throws Exception {
        String req = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "50000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Recipient"
                }
                """;
        MvcResult res = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "ik-s8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isCreated())
                .andReturn();

        String transferId = extractTransferId(res.getResponse().getContentAsString());

        // Pindahkan ke SENDING agar valid menerima callback
        Transfer transfer = transferRepository.findById(transferId).orElseThrow();
        transfer.setStatus(TransferStatus.SENDING);
        String partnerRef = transfer.getPartnerRef();
        transferRepository.save(transfer);

        String callbackPayload = String.format("""
                {
                  "partnerRef": "%s",
                  "eventId": "evt_s8_001",
                  "status": "COMPLETED"
                }
                """, partnerRef);

        // Kirim callback pertama -> Berhasil COMPLETED (200 OK)
        mockMvc.perform(post("/api/callbacks/partner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Kirim callback kedua dengan eventId yang sama persis -> Tetap 200 OK idempoten
        mockMvc.perform(post("/api/callbacks/partner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Transfer finalTransfer = transferRepository.findById(transferId).orElseThrow();
        assertEquals(TransferStatus.COMPLETED, finalTransfer.getStatus());
    }

    // =========================================================================
    // Skenario S9: Partner Retry 3x & Kegagalan Deterministik (FAILED)
    // =========================================================================
    @Test
    @DisplayName("S9: Pembuatan Transfer dengan partnerRef gagal deterministik -> FAILED setelah 3 percobaan")
    void testScenario9_partnerFailureAndRetry() throws Exception {
        // partnerRef berakhiran digit '0' deterministik gagal pada PartnerClient
        String req = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "50000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Recipient",
                  "partnerRef": "PARTNER-REF-FAIL-0"
                }
                """;

        MvcResult res = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "ik-s9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isCreated())
                .andReturn();

        String transferId = extractTransferId(res.getResponse().getContentAsString());

        // Jalankan retry pengiriman partner secara sinkron untuk verifikasi hasil
        partnerService.processPartnerSending(transferId);

        Transfer finalTransfer = transferRepository.findById(transferId).orElseThrow();
        assertEquals(TransferStatus.FAILED, finalTransfer.getStatus());
        assertEquals("Partner rejected after 3 attempts", finalTransfer.getFailReason());

        // Verifikasi melalui endpoint GET
        mockMvc.perform(get("/api/transfers/" + transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failReason").value("Partner rejected after 3 attempts"));
    }
}
