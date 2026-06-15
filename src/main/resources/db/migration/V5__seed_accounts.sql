-- Seed accounts. There is no "create account" API in the spec, so we provision a
-- working set here. On a fresh DB, IDENTITY assigns ids 1..6 deterministically.
--
--   1 USER       (PKR)  payer
--   2 MERCHANT   (PKR)  payee
--   3 FEES       (PKR)  fee sink
--   4 USER       (PKR)  transfer counterparty
--   5 SETTLEMENT (PKR)  bank/settlement boundary (may go negative: it is the
--                        edge of the system where external money enters/leaves)
--   6 MERCHANT   (USD)  multi-currency demo account
INSERT INTO accounts(type, currency) VALUES
    ('USER','PKR'),
    ('MERCHANT','PKR'),
    ('FEES','PKR'),
    ('USER','PKR'),
    ('SETTLEMENT','PKR'),
    ('MERCHANT','USD');

-- Opening balances: inject external funds into the two USER wallets from the
-- settlement boundary. Still a balanced double-entry transaction (debit == credit).
-- 100000000 minor units = 1,000,000.00 PKR each.
WITH t AS (
    INSERT INTO transactions(type, status, idempotency_key, currency, amount)
    VALUES ('TRANSFER','COMPLETED','seed-open-user-1','PKR',100000000)
    RETURNING id
)
INSERT INTO ledger_entries(transaction_id, account_id, direction, amount, currency)
SELECT id, 5, 'DEBIT',  100000000, 'PKR' FROM t
UNION ALL
SELECT id, 1, 'CREDIT', 100000000, 'PKR' FROM t;

WITH t AS (
    INSERT INTO transactions(type, status, idempotency_key, currency, amount)
    VALUES ('TRANSFER','COMPLETED','seed-open-user-4','PKR',100000000)
    RETURNING id
)
INSERT INTO ledger_entries(transaction_id, account_id, direction, amount, currency)
SELECT id, 5, 'DEBIT',  100000000, 'PKR' FROM t
UNION ALL
SELECT id, 4, 'CREDIT', 100000000, 'PKR' FROM t;
