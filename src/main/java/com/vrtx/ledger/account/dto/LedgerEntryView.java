package com.vrtx.ledger.account.dto;

import com.vrtx.ledger.ledger.EntryDirection;
import com.vrtx.ledger.ledger.LedgerEntry;

import java.time.Instant;

public record LedgerEntryView(
        Long entryId,
        Long transactionId,
        EntryDirection direction,
        long amount,
        String currency,
        Instant createdAt
) {
    public static LedgerEntryView from(LedgerEntry e) {
        return new LedgerEntryView(e.getId(), e.getTransaction().getId(),
                e.getDirection(), e.getAmount(), e.getCurrency(), e.getCreatedAt());
    }
}
