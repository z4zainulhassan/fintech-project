-- One row per business operation (payment/refund/transfer/settlement).
-- idempotency_key is UNIQUE: the database is the source of truth for "seen this before".
-- related_transaction_id implements transaction linking (refund -> original payment).
CREATE TABLE transactions (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type                    VARCHAR(20)  NOT NULL CHECK (type IN ('PAYMENT','REFUND','TRANSFER','SETTLEMENT')),
    status                  VARCHAR(20)  NOT NULL CHECK (status IN ('COMPLETED','FAILED')),
    idempotency_key         VARCHAR(120) NOT NULL,
    related_transaction_id  BIGINT       REFERENCES transactions(id),
    currency                VARCHAR(3)      NOT NULL,
    amount                  BIGINT       NOT NULL CHECK (amount > 0),  -- principal, minor units
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_transactions_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_transactions_related ON transactions(related_transaction_id);
