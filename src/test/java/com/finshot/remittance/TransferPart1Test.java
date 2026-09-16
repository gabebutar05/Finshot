package com.finshot.remittance;

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
 * Automated Integration Test untuk Part 1 (Skenario S1 - S6).
 *
 * Menguji seluruh alur HTTP API, State Machine, Fee Calculation,
 * Idempotency, Validasi, dan Error Response secara otomatis.
 *
 * Padanan di C#/.NET: WebApplicationFactory<Program> + xUnit.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TransferPart1Test {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private TransferRepository transferRepository;

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
    // Skenario S1: Happy Path (Pembuatan Transfer Sukses & Read Detail)
    // =========================================================================
    @Test
    @DisplayName("S1: Pembuatan transfer sukses (500.000 KRW -> 20.600 PHP) dan status REQUESTED")
    void scenarioS1_happyPathCreateAndGet() throws Exception {
        String requestJson = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "500000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Juan Dela Cruz"
                }
                """;

        // 1. Kirim POST /api/transfers
        MvcResult result = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId").isNotEmpty())
                .andExpect(jsonPath("$.sendAmount").value("500000"))
                .andExpect(jsonPath("$.fee").value("5000"))             // 1% dari 500.000 = 5.000
                .andExpect(jsonPath("$.totalDebit").value("505000"))     // 500.000 + 5.000
                .andExpect(jsonPath("$.receiveAmount").value("20600.00"))// 500.000 * 0.0412
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn();

        String transferId = extractTransferId(result.getResponse().getContentAsString());
        assertNotNull(transferId);

        // 2. Kirim GET /api/transfers/{id} untuk verifikasi read
        mockMvc.perform(get("/api/transfers/" + transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(transferId))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    // =========================================================================
    // Skenario S2: Minimum Fee 3.000 KRW Enforcement
    // =========================================================================
    @Test
    @DisplayName("S2: Nominal kecil (10.000 KRW) harus dikenakan fee minimum 3.000 KRW (bukan 100 KRW)")
    void scenarioS2_minimumFeeEnforcement() throws Exception {
        String requestJson = """
                {
                  "customerId": "C002",
                  "sendCurrency": "KRW",
                  "sendAmount": "10000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Maria Santos"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-s2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sendAmount").value("10000"))
                .andExpect(jsonPath("$.fee").value("3000"))             // Fee minimum 3.000 KRW terpasang!
                .andExpect(jsonPath("$.totalDebit").value("13000"))     // 10.000 + 3.000
                .andExpect(jsonPath("$.receiveAmount").value("412.00")); // 10.000 * 0.0412
    }

    // =========================================================================
    // Skenario S3: Idempotency (Replay Sukses & Deteksi Konflik 409)
    // =========================================================================
    @Test
    @DisplayName("S3: Idempotency replay mengembalikan transfer lama, tapi payload beda ditolak HTTP 409")
    void scenarioS3_idempotencyReplayAndConflict() throws Exception {
        String originalJson = """
                {
                  "customerId": "C003",
                  "sendCurrency": "KRW",
                  "sendAmount": "100000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Pedro Penduko"
                }
                """;

        // Step 1: Kirim request awal
        MvcResult res1 = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-s3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalJson))
                .andExpect(status().isCreated())
                .andReturn();

        String id1 = extractTransferId(res1.getResponse().getContentAsString());

        // Step 2: Kirim ulang key sama + data sama persis (Idempotency replay)
        MvcResult res2 = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-s3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(originalJson))
                .andExpect(status().isCreated())
                .andReturn();

        String id2 = extractTransferId(res2.getResponse().getContentAsString());
        assertEquals(id1, id2, "Transfer ID harus sama (tidak boleh membuat record baru)");

        // Step 3: Kirim ulang key sama + data BERBEDA (sendAmount jadi 200000)
        String modifiedJson = """
                {
                  "customerId": "C003",
                  "sendCurrency": "KRW",
                  "sendAmount": "200000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Pedro Penduko"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-s3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modifiedJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // =========================================================================
    // Skenario S4: State Machine (Cancel Transfer)
    // =========================================================================
    @Test
    @DisplayName("S4: Status REQUESTED berhasil di-cancel, tapi cancel berulang ditolak HTTP 409")
    void scenarioS4_cancelTransferLifecycle() throws Exception {
        String requestJson = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "50000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Cardo Dalisay"
                }
                """;

        MvcResult createRes = mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-s4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String transferId = extractTransferId(createRes.getResponse().getContentAsString());

        // 1. Batalkan transfer (REQUESTED -> CANCELLED)
        mockMvc.perform(post("/api/transfers/" + transferId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 2. Coba batalkan lagi (CANCELLED -> CANCELLED tidak diizinkan -> 409 Conflict)
        mockMvc.perform(post("/api/transfers/" + transferId + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // =========================================================================
    // Skenario S5: Validasi Input Gagal (HTTP 400)
    // =========================================================================
    @Test
    @DisplayName("S5: Input tidak valid ditolak dengan HTTP 400 VALIDATION_ERROR")
    void scenarioS5_inputValidations() throws Exception {
        // Kasus 1: Idempotency-Key tidak dikirim
        String req1 = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "50000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Name"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Kasus 2: Mata uang salah (USD -> PHP)
        String req2 = """
                {
                  "customerId": "C001",
                  "sendCurrency": "USD",
                  "sendAmount": "50000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Name"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-val-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Kasus 3: Nominal tidak valid (angka negatif)
        String req3 = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "-5000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Name"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-val-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Kasus 4: Nominal di bawah batas minimal (5.000 < 10.000 KRW)
        String req4 = """
                {
                  "customerId": "C001",
                  "sendCurrency": "KRW",
                  "sendAmount": "5000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Name"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-val-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req4))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        // Kasus 5: Customer tidak terdaftar (C999)
        String req5 = """
                {
                  "customerId": "C999",
                  "sendCurrency": "KRW",
                  "sendAmount": "50000",
                  "receiveCurrency": "PHP",
                  "recipientName": "Name"
                }
                """;
        mockMvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "test-key-val-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req5))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // =========================================================================
    // Skenario S6: Resource Not Found (HTTP 404)
    // =========================================================================
    @Test
    @DisplayName("S6: ID transfer tidak ditemukan menghasilkan HTTP 404 NOT_FOUND")
    void scenarioS6_resourceNotFound() throws Exception {
        mockMvc.perform(get("/api/transfers/non-existent-uuid-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(post("/api/transfers/non-existent-uuid-12345/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
