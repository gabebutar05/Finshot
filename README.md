# Finshot Remittance Transfer API

Backend Remittance Transfer API implementation for Finshot Take-Home Assignment (Version 3.11). Supports Part 1 (Required) and Part 2 (Optional).

---

## 1. How to Run (ONE command)

To start the application on port 8080:

```bash
# Windows (PowerShell / CMD)
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

To run all automated tests and verify scenarios S1 through S9:

```bash
.\mvnw.cmd test
```

---

## 2. Technology Stack

- **Language & Runtime:** Java 17+
- **Framework:** Spring Boot (Spring WebMVC, Spring Data JPA, Spring Validation)
- **Database:** H2 in-memory database (`jdbc:h2:mem:finshotdb;DB_CLOSE_DELAY=-1`)
- **Persistence & Migration:** Hibernate ORM with SQL schema initialization via `schema.sql`

---

## 3. How a Client Retries a Create Request Safely

To safely retry a transfer creation (e.g., after network timeouts, dropped connections, or double-clicks):

1. **Required Header:** The client must send an `Idempotency-Key` header with each `POST /api/transfers` request (e.g., `Idempotency-Key: ik_d83e2a10-8fa4-46c5-9273`).
2. **Generation Strategy:** The client should generate a unique identifier per logical transfer intent, such as a **UUID v4** or a unique internal order reference, created once before the first network attempt and reused unchanged across retries.
3. **Server Enforcement & Behaviors:**
   - **Identical Retry (Same Key, Same Data):** The server returns the original transfer (`201 Created` or `200 OK`) without duplicating the transaction.
   - **Reused Key with Different Data:** The server detects payload divergence via a SHA-256 request fingerprint and immediately responds with **HTTP 409 CONFLICT** (`{"error": "CONFLICT", "message": "Idempotency key reused with different request data"}`).
   - **Missing Key:** If the client omits the header, the server rejects the request with **HTTP 400 VALIDATION_ERROR** (`{"error": "VALIDATION_ERROR", "message": "Idempotency-Key header is required"}`).
   - **Concurrent Duplicate Requests:** Database-level `UNIQUE (idempotency_key)` constraint ensures that only one request can persist, while concurrent duplicates are safely reconciled to the existing transfer.

---

## 4. AI Tools Used

1. **Tool Used:** Google Antigravity (Gemini AI Coding Assistant).
2. **Where Used:** Used for assignment requirements review, architectural verification, and drafting test scenarios (S1–S9).
3. **Where Used:** Assisted with verifying pessimistic locking and race-condition handling for the Asia/Seoul daily limit.

---

## 5. What Was Not Implemented (Out of Scope per Brief)

Per the assignment specification instructions (Part 1 & 2):
- **User Authentication / Authorization:** No user login, JWT, or role-based security was implemented.
- **List / Pagination / Search Endpoints:** Only required endpoints (`POST /api/transfers`, `GET /api/transfers/{id}`, `POST /api/transfers/{id}/cancel`, and `POST /api/callbacks/partner`) were implemented.
- **Docker, CI/CD, OpenAPI/Swagger:** Kept lightweight without external infrastructure.
- **Real Bank Partner Integration:** Used an in-memory, deterministic `PartnerClient` adhering to the failure formula `fails when abs(partnerRef.hashCode()) % 10 < 3` and last digit 0–2.
- **Custom Error Frameworks:** Reused the strict JSON shape `{ "error": "...", "message": "..." }` required by specification 1.3.
