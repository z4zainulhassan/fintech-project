-- Accounts. Balance is NEVER stored here; it is derived from ledger_entries.
-- The row exists primarily as an identity + currency holder and as a lock target
-- (SELECT ... FOR UPDATE) to serialise concurrent postings on the same account.
CREATE TABLE accounts (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('USER','MERCHANT','FEES','SETTLEMENT')),
    currency    VARCHAR(3)      NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
