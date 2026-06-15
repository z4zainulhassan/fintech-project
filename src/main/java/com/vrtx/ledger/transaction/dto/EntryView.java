package com.vrtx.ledger.transaction.dto;

import com.vrtx.ledger.ledger.EntryDirection;

/** A single ledger entry as echoed back in a transaction response. */
public record EntryView(Long accountId, EntryDirection direction, long amount) { }
