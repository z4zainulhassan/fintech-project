package com.vrtx.ledger.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long id) {
        super("Transaction not found: " + id);
    }
}
