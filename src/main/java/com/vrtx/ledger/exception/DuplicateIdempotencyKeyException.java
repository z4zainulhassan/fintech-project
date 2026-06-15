package com.vrtx.ledger.exception;

/**
 * Thrown only on the rare concurrent race where two requests with the same
 * Idempotency-Key insert at the same instant and the DB UNIQUE constraint rejects
 * the second. Callers recover by re-reading the original transaction.
 * It never surfaces to the client as an error.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {
    private final String idempotencyKey;
    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super("Duplicate idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }
    public String getIdempotencyKey() { return idempotencyKey; }
}
