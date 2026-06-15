package com.vrtx.ledger.exception;

public class UnbalancedTransactionException extends RuntimeException {
    public UnbalancedTransactionException(long debits, long credits) {
        super("Unbalanced transaction: debits=" + debits + " credits=" + credits);
    }
}
