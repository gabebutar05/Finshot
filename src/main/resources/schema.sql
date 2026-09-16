CREATE TABLE customers (
                           customer_id VARCHAR(10) PRIMARY KEY,
                           name VARCHAR(100) NOT NULL
);

CREATE TABLE transfers (
                           id VARCHAR(36) PRIMARY KEY,
                           customer_id VARCHAR(10) NOT NULL,
                           idempotency_key VARCHAR(100) NOT NULL,
                           request_hash VARCHAR(64) NOT NULL,
                           send_currency VARCHAR(3) NOT NULL,
                           send_amount BIGINT NOT NULL,
                           fee BIGINT NOT NULL,
                           total_debit BIGINT NOT NULL,
                           receive_currency VARCHAR(3) NOT NULL,
                           receive_amount DECIMAL(18,2) NOT NULL,
                           recipient_name VARCHAR(100) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           created_at TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP NOT NULL,

                           CONSTRAINT fk_transfer_customer
                               FOREIGN KEY (customer_id)
                                   REFERENCES customers(customer_id),

                           CONSTRAINT uq_transfer_idempotency
                               UNIQUE (idempotency_key)
);

CREATE INDEX idx_transfers_customer_id
    ON transfers(customer_id);

CREATE INDEX idx_transfers_status
    ON transfers(status);

INSERT INTO customers (customer_id, name)
VALUES ('C001', 'Kim');

INSERT INTO customers (customer_id, name)
VALUES ('C002', 'Lee');

INSERT INTO customers (customer_id, name)
VALUES ('C003', 'Park');