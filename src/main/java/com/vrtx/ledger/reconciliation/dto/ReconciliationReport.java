package com.vrtx.ledger.reconciliation.dto;

import java.util.List;

public record ReconciliationReport(
        boolean balanced,                 // true if the whole ledger is consistent
        long totalDebits,
        long totalCredits,
        long difference,                  // totalDebits - totalCredits (should be 0)
        int unbalancedTransactionCount,
        List<UnbalancedTransactionDto> unbalancedTransactions
) {
    public record UnbalancedTransactionDto(Long transactionId, long debits, long credits) { }
}
