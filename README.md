# VRTX Ledger System

A double-entry, append-only ledger backend for a fintech-style payment system, built with
**Java 17 + Spring Boot 3.3 + PostgreSQL**. Every money movement is recorded as balanced
debit/credit entries; balances are derived from the ledger, never stored; and the system is
safe under concurrency and idempotent by design.

---

## 1. Requirements coverage

| Requirement | Where |
|---|---|
| Double-entry, `sum(debits) == sum(credits)` | `LedgerService.post` (balanced invariant) |
| Append-only / immutable ledger | DB triggers in `V4__append_only_guard.sql` + immutable entities |
| Account types USER / MERCHANT / FEES / SETTLEMENT | `AccountType` |
| Card payment (100 → merchant 97 + fees 3) | `PaymentService` |
| Refund / reversal (+ partial) | `RefundService` |
| Wallet transfer | `TransferService` |
| Merchant settlement | `SettlementService` |
| Fees handling | `FeeCalculator` (+ configurable rate) |
| Balanced transactions | `LedgerService` unbalanced guard |
| Idempotency via `idempotency_key` | UNIQUE constraint + fast-path replay + race handler |
| Concurrency handling | `SELECT … FOR UPDATE` in account-id order (deadlock-safe) |
| Atomic operations | single `@Transactional` posting unit |
| Transaction linking | `related_transaction_id` (refund → payment) |
| Ledger-based balance calculation | `LedgerEntryRepository.balanceOf` |
| Reconciliation | `ReconciliationService` + `GET /reconciliation` |
| **Bonus** configurable fees | `ledger.fees.default-rate-bps` |
| **Bonus** event-driven design | `TransactionPostedEvent` + `@TransactionalEventListener(AFTER_COMMIT)` |
| **Bonus** multi-currency | per-account currency + single-currency validation (no FX) |

---

## 2. Run it

### Prerequisites
- JDK 17, Maven 3.9+

### Start the database
```
Postgres listens on `localhost:5432` (db `vrtx_ledger`, user/pass `postgres`/`postgres`).
Flyway runs all migrations on app startup, including a seed of 6 accounts.

### Run the app 
```
App starts on `http://localhost:8080`. Override DB via env: `DB_URL`, `DB_USER`, `DB_PASSWORD`.

### Run the tests (spins up its own Postgres)

### Seeded accounts
| id | type | currency | starting balance |
|----|------|----------|------------------|
| 1 | USER | PKR | 1,000,000.00 |
| 2 | MERCHANT | PKR | 0 |
| 3 | FEES | PKR | 0 |
| 4 | USER | PKR | 1,000,000.00 |
| 5 | SETTLEMENT | PKR | (funding boundary, may be negative) |
| 6 | MERCHANT | USD | 0 |

All amounts are **minor units** (paisa/cents) as `BIGINT`. `10000` = `100.00`.

---

## 3. API

All four POST endpoints require an `Idempotency-Key` header. Retrying with the same key
returns the original transaction (HTTP 200) instead of creating a new one.

### Payment — `POST /transactions/payment`
`feeAmount` is optional; if omitted the configured rate (default 3%) is applied.
```bash
curl -i -X POST http://localhost:8080/transactions/payment \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: pay-001' \
  -d '{"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,"amount":10000,"feeAmount":300,"currency":"PKR"}'
```
`201`:
```json
{"transactionId":3,"type":"PAYMENT","status":"COMPLETED","relatedTransactionId":null,
 "currency":"PKR","amount":10000,"createdAt":"2026-06-11T10:00:00Z",
 "entries":[{"accountId":1,"direction":"DEBIT","amount":10000},
            {"accountId":2,"direction":"CREDIT","amount":9700},
            {"accountId":3,"direction":"CREDIT","amount":300}]}
```

### Refund — `POST /transactions/refund`
Partial refunds supported; cumulative refunds cannot exceed the original principal.
```bash
curl -i -X POST http://localhost:8080/transactions/refund \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: ref-001' \
  -d '{"originalTransactionId":3,"amount":10000}'
```

### Transfer — `POST /transactions/transfer`
```bash
curl -i -X POST http://localhost:8080/transactions/transfer \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: trf-001' \
  -d '{"fromAccountId":1,"toAccountId":4,"amount":5000,"currency":"PKR"}'
```

### Settlement — `POST /transactions/settlement`
```bash
curl -i -X POST http://localhost:8080/transactions/settlement \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: set-001' \
  -d '{"merchantAccountId":2,"settlementAccountId":5,"amount":9700,"currency":"PKR"}'
```

### Account ledger — `GET /accounts/{id}/ledger`
```bash
curl http://localhost:8080/accounts/1/ledger
```
```json
{"accountId":1,"type":"USER","currency":"PKR","balance":99990000,
 "entries":[{"entryId":2,"transactionId":1,"direction":"CREDIT","amount":100000000,"currency":"PKR","createdAt":"..."},
            {"entryId":7,"transactionId":3,"direction":"DEBIT","amount":10000,"currency":"PKR","createdAt":"..."}]}
```

### Reconciliation — `GET /reconciliation`
```json
{"balanced":true,"totalDebits":210010000,"totalCredits":210010000,
 "difference":0,"unbalancedTransactionCount":0,"unbalancedTransactions":[]}
```

### Error envelope (consistent everywhere)
```json
{"timestamp":"2026-06-11T10:00:00Z","status":422,"error":"INSUFFICIENT_FUNDS",
 "message":"Insufficient funds in account 1","path":"/transactions/payment"}
```

| Case | HTTP | error code |
|---|---|---|
| Created | 201 | — |
| Idempotent replay (seq or concurrent) | 200 | — (original body) |
| Validation (missing field, non-positive, missing key) | 400 | `VALIDATION_ERROR` |
| Insufficient funds | 422 | `INSUFFICIENT_FUNDS` |
| Over-refund / non-payment refund | 422 | `REFUND_NOT_ALLOWED` |
| Currency mismatch | 422 | `CURRENCY_MISMATCH` |
| Internal unbalanced guard | 422 | `UNBALANCED_TRANSACTION` |
| Account / transaction not found | 404 | `NOT_FOUND` |

---

### Edge cases handled
- Duplicate idempotency key (sequential **and** concurrent)
- Insufficient funds (incl. concurrent overdraft race → balance never negative)
- Over-refund, double-refund, refund of a non-payment
- Currency mismatch (multi-currency accounts, no implicit FX)
- Self-transfer rejected
- Missing idempotency key, non-positive amounts, malformed body
- Unknown account / transaction
