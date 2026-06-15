package com.vrtx.ledger.reconciliation;

/** Projection for a transaction whose debits and credits do not match. */
public interface UnbalancedTransaction {
    Long getTransactionId();
    Long getDebits();
    Long getCredits();
}
