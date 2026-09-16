# DESIGN.md

### Q1. Describe your tables. Why these indexes and constraints? Which constraint stops which problem?

We use three tables:
1. `customers`: Stores customer ID and name.
2. `transfers`: Stores the transfer lifecycle, financial amounts, idempotency tokens, and status.
3. `partner_callbacks`: Tracks processed partner callback event IDs.

**Constraints & Problems Solved:**
- `PRIMARY KEY (id)` on `transfers`: Guarantees unique identity per transfer.
- `FOREIGN KEY (customer_id) REFERENCES customers`: Prevents orphan transfers for invalid/non-existent customers.
- `UNIQUE (idempotency_key)`: Prevents duplicate transfer creation at the database engine level during concurrent requests.
- `PRIMARY KEY (event_id)` on `partner_callbacks`: Prevents duplicate callback event execution.

**Indexes & Rationale:**
- `idx_transfers_customer_id`: Accelerates customer transfer lookups and daily limit aggregations.
- `idx_transfers_status`: Speeds up state filtering (excluding CANCELLED/FAILED).
- `idx_transfers_partner_ref`: Ensures fast lookups when callbacks arrive.
- `idx_transfers_created_at`: Enables rapid date-range filtering for Asia/Seoul daily limit calculations.

---

### Q2. How does your idempotency work? What identifies a retry, and where is it enforced — application code, database, or somewhere else? What happens if two identical create requests arrive at the exact same moment?

- **Identifier:** The client provides an `Idempotency-Key` HTTP header.
- **Mechanism:** We store `idempotency_key` and a SHA-256 `request_hash` (computed from customer ID, currencies, send amount, and recipient name).
- **Enforcement:** Two-tier enforcement across application and database:
  1. **Application:** Checks if the key exists. If found and hash matches, returns original transfer. If hash differs, returns HTTP 409 Conflict.
  2. **Database:** `UNIQUE (idempotency_key)` constraint enforces uniqueness at the storage level.
- **Concurrent Identical Requests:** Both pass the initial application check. One transaction commits the insert; the other encounters a `DataIntegrityViolationException` from the unique constraint. The exception handler catches this, queries the newly inserted record, verifies payload match, and safely returns the existing transfer without creating duplicates.

---

### Q3. How do you represent, store, and round money — in your code, your API, and your database? At which exact step does rounding happen, and why there?

- **Code:** `Long` for KRW whole currency (`sendAmount`, `fee`, `totalDebit`). `BigDecimal` for PHP (`receiveAmount`) to avoid IEEE-754 binary floating-point rounding errors.
- **Database:** `BIGINT` for KRW columns; `DECIMAL(18,2)` for PHP `receiveAmount`.
- **API:** Exact string representations in JSON. `receiveAmount` is formatted with `%.2f` to ensure exactly two decimal places, including trailing zeros (e.g. `"20600.00"`).
- **Rounding Step & Why:**
  - Fee: `sendAmount * 0.01` rounded `HALF_UP` to whole KRW (`setScale(0)`), minimum 3,000 KRW.
  - Receive Amount: `sendAmount * 0.0412` rounded `HALF_UP` to 2 decimal places (`setScale(2)`).
  - Rounding happens immediately in domain service calculation before persisting to the database. This guarantees mathematical consistency (`totalDebit = sendAmount + fee`) and audit integrity between debited and credited values.

---

### Q4. How did you make the daily limit safe when requests arrive at the same time? Name the mechanism you used (row lock, unique constraint, isolation level, something else) and what you gave up by choosing it.

- **Mechanism:** Pessimistic Write Lock (`SELECT ... FOR UPDATE` via JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)`) on the `Customer` row.
- **Operation:** When a transfer request begins, the transaction acquires an exclusive lock on the customer row. It then sums all `sendAmount` for Asia/Seoul today (excluding CANCELLED and FAILED). If `currentSum + newSendAmount > 3,000,000`, the transaction fails with HTTP 422 `LIMIT_EXCEEDED`.
- **Trade-off:** We sacrificed high concurrent throughput for simultaneous transfers initiated by the *exact same customer*, as their requests queue behind the row lock. However, transfers for different customers execute concurrently without contention. In exchange, we gain 100% strict limit enforcement with zero race conditions or optimistic lock retry overhead.

---

### Q5. The partner callback arrives before your code finished saving the result of the partner call. What happens in your code? If it breaks, say so and explain how you would fix it.

- **What Happens:** Our implementation would break:
  1. If `partnerRef` is not yet committed to the database, `transferRepository.findByPartnerRef(partnerRef)` fails with HTTP 404 NOT_FOUND.
  2. If the record exists but transaction commit is in-flight and status is still `REQUESTED`, the callback fails with HTTP 409 CONFLICT because callbacks are only valid from `SENDING`.
- **How to Fix It:**
  1. **Transactional Boundary:** Ensure `partnerRef` and `SENDING` status are committed to the database *before* dispatching the external call to the partner.
  2. **Transactional Outbox / Inbox Pattern:** Store incoming callbacks in an `inbox` table immediately and acknowledge HTTP 200/202. A background worker reconciles the callback against the transfer once state is saved.
  3. **Short Retry Mechanism:** If a callback arrives for an existing transfer in `REQUESTED`, wait/retry for up to 500ms before rejecting.
