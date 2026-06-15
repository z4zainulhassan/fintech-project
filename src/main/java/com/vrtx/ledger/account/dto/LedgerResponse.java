package com.vrtx.ledger.account.dto;

import java.util.List;

public record LedgerResponse(
        Long accountId,
        String type,
        String currency,
        long balance,          // ledger-derived: sum(credits) - sum(debits), minor units
        List<LedgerEntryView> entries
) { }
