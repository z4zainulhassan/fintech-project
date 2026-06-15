package com.vrtx.ledger.transaction.dto;

import com.vrtx.ledger.ledger.LedgerEntry;
import com.vrtx.ledger.transaction.Transaction;
import com.vrtx.ledger.transaction.TransactionStatus;
import com.vrtx.ledger.transaction.TransactionType;

import java.time.Instant;
import java.util.List;

public record TransactionResponse(
        Long transactionId,
        TransactionType type,
        TransactionStatus status,
        Long relatedTransactionId,
        String currency,
        long amount,
        Instant createdAt,
        List<EntryView> entries
) {
    public static TransactionResponse from(Transaction txn, List<LedgerEntry> entries) {
        List<EntryView> views = entries.stream()
                .map(e -> new EntryView(e.getAccount().getId(), e.getDirection(), e.getAmount()))
                .toList();
        return new TransactionResponse(
                txn.getId(), txn.getType(), txn.getStatus(), txn.getRelatedTransactionId(),
                txn.getCurrency(), txn.getAmount(), txn.getCreatedAt(), views);
    }
}
