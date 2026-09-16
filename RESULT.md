# Finshot Remittance API - Execution Results (S1 - S9)

All scenarios were executed sequentially against a freshly started instance of the Finshot Remittance Transfer API.

---

### S1

Request:
```http
POST /api/transfers
Idempotency-Key: ik-req-s1-001
Content-Type: application/json

{
  "customerId": "C001",
  "sendCurrency": "KRW",
  "sendAmount": "500000",
  "receiveCurrency": "PHP",
  "recipientName": "Juan Dela Cruz"
}
```

Status: 201

Response:
```json
{
  "createdAt": "2026-09-16T11:59:36.2150571",
  "fee": "5000",
  "partnerRef": "REF-17895347762159",
  "receiveAmount": "20600.00",
  "sendAmount": "500000",
  "status": "REQUESTED",
  "totalDebit": "505000",
  "transferId": "3ff172d4-1023-4484-803a-06409dda0013"
}
```

---

### S2

Request:
```http
POST /api/transfers
Idempotency-Key: ik-req-s1-001
Content-Type: application/json

{
  "customerId": "C001",
  "sendCurrency": "KRW",
  "sendAmount": "500000",
  "receiveCurrency": "PHP",
  "recipientName": "Juan Dela Cruz"
}
```

Status: 201

Response:
```json
{
  "createdAt": "2026-09-16T11:59:36.215057",
  "fee": "5000",
  "partnerRef": "REF-17895347762159",
  "receiveAmount": "20600.00",
  "sendAmount": "500000",
  "status": "REQUESTED",
  "totalDebit": "505000",
  "transferId": "3ff172d4-1023-4484-803a-06409dda0013"
}
```

---

### S3

Request:
```http
POST /api/transfers
Idempotency-Key: ik-req-s1-001
Content-Type: application/json

{
  "customerId": "C001",
  "sendCurrency": "KRW",
  "sendAmount": "600000",
  "receiveCurrency": "PHP",
  "recipientName": "Juan Dela Cruz"
}
```

Status: 409

Response:
```json
{
  "error": "CONFLICT",
  "message": "Idempotency key reused with different request data"
}
```

---

### S4

Request 1 (Create):
```http
POST /api/transfers
Idempotency-Key: ik-req-s4-001
Content-Type: application/json

{
  "customerId": "C001",
  "sendCurrency": "KRW",
  "sendAmount": "100000",
  "receiveCurrency": "PHP",
  "recipientName": "Maria Santos"
}
```

Status: 201

Response:
```json
{
  "createdAt": "2026-09-16T11:59:36.266384",
  "fee": "3000",
  "partnerRef": "REF-17895347762669",
  "receiveAmount": "4120.00",
  "sendAmount": "100000",
  "status": "REQUESTED",
  "totalDebit": "103000",
  "transferId": "84d1d746-cf31-42dc-ba28-5ae38c3b54bd"
}
```

Request 2 (Cancel):
```http
POST /api/transfers/84d1d746-cf31-42dc-ba28-5ae38c3b54bd/cancel
```

Status: 200

Response:
```json
{
  "createdAt": "2026-09-16T11:59:36.266384",
  "fee": "3000",
  "partnerRef": "REF-17895347762669",
  "receiveAmount": "4120.00",
  "sendAmount": "100000",
  "status": "CANCELLED",
  "totalDebit": "103000",
  "transferId": "84d1d746-cf31-42dc-ba28-5ae38c3b54bd"
}
```

---

### S5

Request:
```http
POST /api/transfers/84d1d746-cf31-42dc-ba28-5ae38c3b54bd/cancel
```

Status: 409

Response:
```json
{
  "error": "CONFLICT",
  "message": "Cannot cancel transfer in status CANCELLED. Only REQUESTED transfers can be cancelled."
}
```

---

### S6

Request:
```http
POST /api/transfers
Idempotency-Key: ik-req-s6-001
Content-Type: application/json

{
  "customerId": "C001",
  "sendCurrency": "KRW",
  "sendAmount": "5000",
  "receiveCurrency": "PHP",
  "recipientName": "Juan Dela Cruz"
}
```

Status: 400

Response:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "sendAmount must be at least 10000"
}
```

---

### S7

#### Last Success (Cumulative: 3,000,000 KRW)
Request:
```http
POST /api/transfers
Idempotency-Key: ik-req-s7-002
Content-Type: application/json

{
  "customerId": "C002",
  "sendCurrency": "KRW",
  "sendAmount": "1000000",
  "receiveCurrency": "PHP",
  "recipientName": "Lee Min-Ho"
}
```

Status: 201

Response:
```json
{
  "createdAt": "2026-09-16T11:59:36.3132973",
  "fee": "10000",
  "partnerRef": "REF-17895347763139",
  "receiveAmount": "41200.00",
  "sendAmount": "1000000",
  "status": "REQUESTED",
  "totalDebit": "1010000",
  "transferId": "408a7790-aeb3-46da-9e66-1d52a51ef15d"
}
```

#### Rejection (Cumulative: 3,010,000 KRW - Exceeds 3,000,000 KRW Daily Limit)
Request:
```http
POST /api/transfers
Idempotency-Key: ik-req-s7-003
Content-Type: application/json

{
  "customerId": "C002",
  "sendCurrency": "KRW",
  "sendAmount": "10000",
  "receiveCurrency": "PHP",
  "recipientName": "Lee Min-Ho"
}
```

Status: 422

Response:
```json
{
  "error": "LIMIT_EXCEEDED",
  "message": "Daily limit of 3000000 KRW exceeded for customer C002"
}
```

---

### S8

#### First Callback Delivery
Request:
```http
POST /api/callbacks/partner
Content-Type: application/json

{
  "partnerRef": "REF-17895347763339",
  "eventId": "evt_0001",
  "status": "COMPLETED"
}
```

Status: 200

Response:
```json
{
  "createdAt": "2026-09-16T11:59:36.333374",
  "fee": "3000",
  "partnerRef": "REF-17895347763339",
  "receiveAmount": "6180.00",
  "sendAmount": "150000",
  "status": "COMPLETED",
  "totalDebit": "153000",
  "transferId": "adf9db33-2137-40bb-b193-7776861d8b45"
}
```

#### Second Callback Delivery (Duplicate eventId)
Request:
```http
POST /api/callbacks/partner
Content-Type: application/json

{
  "partnerRef": "REF-17895347763339",
  "eventId": "evt_0001",
  "status": "COMPLETED"
}
```

Status: 200

Response:
```json
{
  "createdAt": "2026-09-16T11:59:36.333374",
  "fee": "3000",
  "partnerRef": "REF-17895347763339",
  "receiveAmount": "6180.00",
  "sendAmount": "150000",
  "status": "COMPLETED",
  "totalDebit": "153000",
  "transferId": "adf9db33-2137-40bb-b193-7776861d8b45"
}
```

#### Final State
Request:
```http
GET /api/transfers/adf9db33-2137-40bb-b193-7776861d8b45
```

Status: 200

Response:
```json
{
  "createdAt": "2026-09-16T11:59:36.333374",
  "fee": "3000",
  "partnerRef": "REF-17895347763339",
  "receiveAmount": "6180.00",
  "sendAmount": "150000",
  "status": "COMPLETED",
  "totalDebit": "153000",
  "transferId": "adf9db33-2137-40bb-b193-7776861d8b45"
}
```

---

### S9

Request (Create with partnerRef ending in 0):
```http
POST /api/transfers
Idempotency-Key: ik-req-s9-001
Content-Type: application/json

{
  "customerId": "C001",
  "sendCurrency": "KRW",
  "sendAmount": "75000",
  "receiveCurrency": "PHP",
  "recipientName": "Fail Partner Test",
  "partnerRef": "PARTNER-FAIL-REF-0"
}
```

Status: 201

Response:
```json
{
  "createdAt": "2026-09-16T11:59:37.8767763",
  "fee": "3000",
  "partnerRef": "PARTNER-FAIL-REF-0",
  "receiveAmount": "3090.00",
  "sendAmount": "75000",
  "status": "REQUESTED",
  "totalDebit": "78000",
  "transferId": "cb4f531d-8df9-4ac3-89ec-f379371a0779"
}
```

#### Partner Retry Log Output
```log
2026-09-16T11:59:38.880+07:00  INFO 24972 --- [finshot-remittance] [         task-6] c.f.remittance.partner.PartnerService    : Transfer cb4f531d-8df9-4ac3-89ec-f379371a0779 transitioned to SENDING
2026-09-16T11:59:39.084+07:00  WARN 24972 --- [finshot-remittance] [         task-6] c.f.remittance.partner.PartnerService    : [Partner Attempt 1/3] partnerRef: PARTNER-FAIL-REF-0 - Outcome: FAILED (Rejected)
2026-09-16T11:59:39.389+07:00  WARN 24972 --- [finshot-remittance] [         task-6] c.f.remittance.partner.PartnerService    : [Partner Attempt 2/3] partnerRef: PARTNER-FAIL-REF-0 - Outcome: FAILED (Rejected)
2026-09-16T11:59:39.791+07:00  WARN 24972 --- [finshot-remittance] [         task-6] c.f.remittance.partner.PartnerService    : [Partner Attempt 3/3] partnerRef: PARTNER-FAIL-REF-0 - Outcome: FAILED (Rejected)
2026-09-16T11:59:39.795+07:00  WARN 24972 --- [finshot-remittance] [         task-6] c.f.remittance.partner.PartnerService    : All 3 attempts failed. Transfer cb4f531d-8df9-4ac3-89ec-f379371a0779 transitioned to FAILED.
```

#### Final State
Request:
```http
GET /api/transfers/cb4f531d-8df9-4ac3-89ec-f379371a0779
```

Status: 200

Response:
```json
{
  "createdAt": "2026-09-16T11:59:37.876776",
  "failReason": "Partner rejected after 3 attempts",
  "fee": "3000",
  "partnerRef": "PARTNER-FAIL-REF-0",
  "receiveAmount": "3090.00",
  "sendAmount": "75000",
  "status": "FAILED",
  "totalDebit": "78000",
  "transferId": "cb4f531d-8df9-4ac3-89ec-f379371a0779"
}
```
