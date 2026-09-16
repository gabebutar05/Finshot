    package com.finshot.remittance.service;

    import com.finshot.remittance.dto.CreateTransferRequest;
    import com.finshot.remittance.entity.Customer;
    import com.finshot.remittance.entity.Transfer;
    import com.finshot.remittance.repository.CustomerRepository;
    import com.finshot.remittance.repository.TransferRepository;
    import org.springframework.stereotype.Service;

    import java.math.BigDecimal;
    import java.math.RoundingMode;
    import java.time.LocalDateTime;
    import java.util.UUID;

    @Service
    public class TransferService {

        private final CustomerRepository customerRepository;
        private final TransferRepository transferRepository;

        public TransferService(
                CustomerRepository customerRepository,
                TransferRepository transferRepository) {

            this.customerRepository = customerRepository;
            this.transferRepository = transferRepository;
        }

        public Transfer createTransfer(CreateTransferRequest request, String idempotencyKey) {

            // 1. Find customer
            Customer customer = customerRepository
                    .findById(request.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            // 2. Convert send amount
            long sendAmount = Long.parseLong(request.getSendAmount());

            // 3. Calculate fee
            long fee = calculateFee(sendAmount);

            // 4. Calculate total debit
            long totalDebit = sendAmount + fee;

            // 5. Calculate receive amount
            BigDecimal receiveAmount = BigDecimal.valueOf(sendAmount)
                    .multiply(new BigDecimal("0.0412"))
                    .setScale(2, RoundingMode.HALF_UP);

            // 6. Create Transfer
            Transfer transfer = new Transfer();

            transfer.setId(UUID.randomUUID().toString());
            transfer.setCustomerId(customer.getCustomerId());

            transfer.setSendCurrency(request.getSendCurrency());
            transfer.setSendAmount(sendAmount);

            transfer.setFee(fee);
            transfer.setTotalDebit(totalDebit);

            transfer.setReceiveCurrency(request.getReceiveCurrency());
            transfer.setReceiveAmount(receiveAmount);

            transfer.setRecipientName(request.getRecipientName());

            transfer.setStatus("REQUESTED");

            LocalDateTime now = LocalDateTime.now();

            transfer.setCreatedAt(now);
            transfer.setUpdatedAt(now);

            // 7. Save
            return transferRepository.save(transfer);
        }

        private long calculateFee(long sendAmount) {

            BigDecimal fee = BigDecimal.valueOf(sendAmount)
                    .multiply(new BigDecimal("0.01"))
                    .setScale(0, RoundingMode.HALF_UP);

            return Math.max(fee.longValue(), 3000);
        }
    }

