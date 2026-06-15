package com.vrtx.ledger.event;

import com.vrtx.ledger.transaction.TransactionType;

/** Bonus: event-driven design. Published after a transaction commits. */
public record TransactionPostedEvent(Long transactionId, TransactionType type, String currency, long amount) { }
