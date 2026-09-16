package com.finshot.remittance.service;

import com.finshot.remittance.dto.CreateTransferRequest;
import com.finshot.remittance.dto.TransferResponse;
import com.finshot.remittance.entity.Customer;
import com.finshot.remittance.entity.Transfer;
import com.finshot.remittance.entity.TransferStatus;
import com.finshot.remittance.exception.ConflictException;
import com.finshot.remittance.exception.DailyLimitExceededException;
import com.finshot.remittance.exception.ResourceNotFoundException;
import com.finshot.remittance.exception.ValidationException;
import com.finshot.remittance.repository.CustomerRepository;
import com.finshot.remittance.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Layer untuk logika bisnis transfer remitansi (Part 1).
 *
 * Di C#/.NET: Setara dengan TransferService yang di-register via builder.Services.AddScoped<ITransferService, TransferService>().
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    // Konstanta Bisnis Finshot (Part 1.1 / Q3)
    private static final BigDecimal FX_RATE = new BigDecimal("0.0412");
    private static final BigDecimal FEE_PERCENT = new BigDecimal("0.01");
    private static final long MIN_FEE_KRW = 3000L;
    private static final long MIN_SEND_AMOUNT = 10000L;

    // Batas Limit Harian (Part 2 - Limit Enforcement)
    private static final long DAILY_LIMIT_KRW = 3000000L;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final TransferRepository transferRepository;
    private final CustomerRepository customerRepository;
    private final com.finshot.remittance.partner.PartnerService partnerService;

    public TransferService(TransferRepository transferRepository,
                           CustomerRepository customerRepository,
                           com.finshot.remittance.partner.PartnerService partnerService) {
        this.transferRepository = transferRepository;
        this.customerRepository = customerRepository;
        this.partnerService = partnerService;
    }

    /**
     * Membuat transaksi transfer baru (Part 1.3 Create Transfer).
     * Dilengkapi pengecekan Idempotensi (Part 1.3 / Q2) dan Validasi Input Bisnis.
     */
    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request, String idempotencyKey) {
        // 1. Validasi Idempotency-Key
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("Missing required header: Idempotency-Key");
        }

        // 2. Hitung SHA-256 hash dari canonical payload untuk verifikasi idempotency
        String requestHash = computeRequestHash(request);

        // 3. Pengecekan Idempotency-Key di Database
        Optional<Transfer> existingOpt = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            Transfer existing = existingOpt.get();
            // Jika payload sama persis, kembalikan data transaksi yang sudah ada (HTTP 200/201)
            if (existing.getRequestHash().equals(requestHash)) {
                log.info("Idempotent request replayed for key: {}", idempotencyKey);
                return TransferResponse.fromEntity(existing);
            }
            // Jika payload berbeda dengan key yang sama, tolak dengan 409 Conflict (Scenario S3)
            throw new ConflictException("Idempotency key reused with different request data");
        }

        // 4. Validasi Format dan Nilai Input Request (Scenario S2 & S5)
        validateCreateRequest(request);

        // 5. Validasi Customer dan Lock untuk mencegah race condition (Scenario S5 & S7)
        Customer customer = customerRepository.findByIdForUpdate(request.getCustomerId())
                .orElseThrow(() -> new ValidationException("unknown customerId: " + request.getCustomerId()));

        long sendAmount = Long.parseLong(request.getSendAmount());

        // 6. Cek batas limit harian di zona Asia/Seoul (UTC+9)
        checkDailyLimit(customer.getCustomerId(), sendAmount);

        // 7. Hitung Biaya (Fee), Total Debit, dan Receive Amount (Part 1.1 / Q3)
        long fee = calculateFee(sendAmount);
        long totalDebit = sendAmount + fee;
        BigDecimal receiveAmount = calculateReceiveAmount(sendAmount);

        // 8. Buat entitas Transfer baru dengan status awal REQUESTED
        Transfer transfer = new Transfer();
        transfer.setId(UUID.randomUUID().toString());
        transfer.setCustomerId(customer.getCustomerId());
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setRequestHash(requestHash);

        // Partner reference (bisa dari request atau auto-generate)
        String partnerRef = (request.getPartnerRef() != null && !request.getPartnerRef().isBlank())
                ? request.getPartnerRef()
                : generateDefaultPartnerRef();
        transfer.setPartnerRef(partnerRef);

        transfer.setSendCurrency(request.getSendCurrency());
        transfer.setSendAmount(sendAmount);
        transfer.setFee(fee);
        transfer.setTotalDebit(totalDebit);
        transfer.setReceiveCurrency(request.getReceiveCurrency());
        transfer.setReceiveAmount(receiveAmount);
        transfer.setRecipientName(request.getRecipientName().trim());
        transfer.setStatus(TransferStatus.REQUESTED);

        LocalDateTime now = LocalDateTime.now();
        transfer.setCreatedAt(now);
        transfer.setUpdatedAt(now);

        // 9. Simpan ke Database
        Transfer saved;
        try {
            saved = transferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException ex) {
            // Menangani race condition jika dua request identik tiba bersamaan di thread berbeda
            Transfer concurrent = transferRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
            if (concurrent.getRequestHash().equals(requestHash)) {
                return TransferResponse.fromEntity(concurrent);
            }
            throw new ConflictException("Idempotency key reused with different request data");
        }

        // 10. Picu proses pengiriman partner secara asinkron di background (Part 2.1)
        partnerService.initiatePartnerSendingAsync(saved.getId());

        log.info("Transfer created: id={}, customerId={}, amount={}", saved.getId(), saved.getCustomerId(), saved.getSendAmount());
        return TransferResponse.fromEntity(saved);
    }

    /**
     * Mengambil detail transaksi berdasarkan ID (Part 1.3 Read Transfer).
     */
    @Transactional(readOnly = true)
    public TransferResponse getTransfer(String transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));
        return TransferResponse.fromEntity(transfer);
    }

    /**
     * Membatalkan transaksi (Part 1.3 Cancel Transfer).
     * Sesuai aturan State Machine (Part 1.2): Hanya transaksi berstatus REQUESTED yang boleh di-cancel.
     */
    @Transactional
    public TransferResponse cancelTransfer(String transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));

        if (transfer.getStatus() != TransferStatus.REQUESTED) {
            throw new ConflictException("Cannot cancel transfer in status " + transfer.getStatus() +
                    ". Only REQUESTED transfers can be cancelled.");
        }

        transfer.setStatus(TransferStatus.CANCELLED);
        transfer.setUpdatedAt(LocalDateTime.now());
        Transfer updated = transferRepository.save(transfer);

        log.info("Transfer {} successfully cancelled.", transferId);
        return TransferResponse.fromEntity(updated);
    }

    // ==========================================
    // Helper Methods & Business Calculations
    // ==========================================

    private void validateCreateRequest(CreateTransferRequest request) {
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new ValidationException("customerId is required");
        }

        if (!"KRW".equals(request.getSendCurrency()) || !"PHP".equals(request.getReceiveCurrency())) {
            throw new ValidationException("sendCurrency must be KRW and receiveCurrency must be PHP");
        }

        if (request.getRecipientName() == null || request.getRecipientName().isBlank() || request.getRecipientName().length() > 100) {
            throw new ValidationException("recipientName must not be empty and must not exceed 100 characters");
        }

        if (request.getSendAmount() == null || request.getSendAmount().isBlank()) {
            throw new ValidationException("sendAmount is required");
        }

        // Pastikan hanya angka bilangan bulat positif tanpa desimal
        if (!request.getSendAmount().matches("^[0-9]+$")) {
            throw new ValidationException("sendAmount must be a positive integer without decimals");
        }

        long amount;
        try {
            amount = Long.parseLong(request.getSendAmount());
        } catch (NumberFormatException e) {
            throw new ValidationException("sendAmount is not a valid number");
        }

        if (amount < MIN_SEND_AMOUNT) {
            throw new ValidationException("sendAmount must be at least 10000");
        }
    }

    private void checkDailyLimit(String customerId, long newSendAmount) {
        LocalDate todaySeoul = LocalDate.now(SEOUL_ZONE);
        LocalDateTime startOfDay = todaySeoul.atStartOfDay();
        LocalDateTime endOfDay = todaySeoul.plusDays(1).atStartOfDay();

        long currentDailyTotal = transferRepository.sumSendAmountByCustomerAndDate(
                customerId,
                startOfDay,
                endOfDay
        );

        if (currentDailyTotal + newSendAmount > DAILY_LIMIT_KRW) {
            throw new DailyLimitExceededException(
                    "Daily limit of " + DAILY_LIMIT_KRW + " KRW exceeded for customer " + customerId
            );
        }
    }

    /**
     * Perhitungan fee: 1% dari sendAmount, minimal 3,000 KRW, dibulatkan HALF_UP ke KRW bulat.
     */
    private long calculateFee(long sendAmount) {
        BigDecimal calculated = BigDecimal.valueOf(sendAmount)
                .multiply(FEE_PERCENT)
                .setScale(0, RoundingMode.HALF_UP);
        return Math.max(calculated.longValue(), MIN_FEE_KRW);
    }

    /**
     * Perhitungan receiveAmount: sendAmount * 0.0412, tepat 2 desimal HALF_UP.
     */
    private BigDecimal calculateReceiveAmount(long sendAmount) {
        return BigDecimal.valueOf(sendAmount)
                .multiply(FX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Menghitung sidik jari unik (SHA-256) dari isi request.
     */
    private String computeRequestHash(CreateTransferRequest request) {
        String raw = request.getCustomerId() + "|" +
                request.getSendCurrency() + "|" +
                request.getSendAmount() + "|" +
                request.getReceiveCurrency() + "|" +
                (request.getRecipientName() != null ? request.getRecipientName().trim() : "");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String generateDefaultPartnerRef() {
        return "REF-" + System.currentTimeMillis() + "9";
    }
}
