DROP TABLE IF EXISTS partner_callbacks;
DROP TABLE IF EXISTS transfers;
DROP TABLE IF EXISTS customers;

-- 1. Tabel customers (Diberikan oleh Finshot sebagai data awal)
CREATE TABLE customers (
    customer_id VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL
);

-- 2. Tabel transfers (Dirancang untuk menyimpan seluruh data pengiriman uang)
CREATE TABLE transfers (
    id               VARCHAR(36) PRIMARY KEY,
    customer_id      VARCHAR(10) NOT NULL,
    idempotency_key  VARCHAR(100) NOT NULL,
    request_hash     VARCHAR(64) NOT NULL,
    partner_ref      VARCHAR(100),
    send_currency    VARCHAR(3) NOT NULL,
    send_amount      BIGINT NOT NULL,
    fee              BIGINT NOT NULL,
    total_debit      BIGINT NOT NULL,
    receive_currency VARCHAR(3) NOT NULL,
    receive_amount   DECIMAL(18,2) NOT NULL,
    recipient_name   VARCHAR(100) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    fail_reason      VARCHAR(255),
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,

    -- Foreign key: Mencegah transaksi tanpa nasabah yang terdaftar
    CONSTRAINT fk_transfer_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id),

    -- Unique Constraint: Benteng pertahanan database agar tidak ada 2 transfer dengan idempotency_key yang sama
    CONSTRAINT uq_transfer_idempotency
        UNIQUE (idempotency_key)
);

-- Indexing untuk mempercepat query:
-- a. Pencarian dan akumulasi limit harian per customer
CREATE INDEX idx_transfers_customer_id ON transfers(customer_id);

-- b. Filter berdasarkan status transaksi (misal: exclude CANCELLED/FAILED)
CREATE INDEX idx_transfers_status ON transfers(status);

-- c. Pencarian cepat saat webhook callback dari partner tiba membawa partner_ref
CREATE INDEX idx_transfers_partner_ref ON transfers(partner_ref);

-- d. Pencarian rentang waktu tanggal di Seoul (Asia/Seoul)
CREATE INDEX idx_transfers_created_at ON transfers(created_at);

-- 3. Tabel partner_callbacks (Mencatat event_id agar webhook partner bersifat idempoten)
CREATE TABLE partner_callbacks (
    event_id     VARCHAR(100) PRIMARY KEY,
    partner_ref  VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);

-- Data Awal Nasabah (Sesuai soal Finshot 1.4)
INSERT INTO customers (customer_id, name) VALUES
    ('C001', 'Kim'),
    ('C002', 'Lee'),
    ('C003', 'Park');
