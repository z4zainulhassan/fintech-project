-- The ledger. Append-only, immutable. Every money movement is two or more rows.
-- amount is in MINOR UNITS (paisa/cents) as BIGINT -> no floating point money.
CREATE TABLE ledger_entries (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id  BIGINT       NOT NULL REFERENCES transactions(id),
    account_id      BIGINT       NOT NULL REFERENCES accounts(id),
    direction       VARCHAR(6)   NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    amount          BIGINT       NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_txn     ON ledger_entries(transaction_id);
