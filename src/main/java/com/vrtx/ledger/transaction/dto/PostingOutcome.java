package com.vrtx.ledger.transaction.dto;

/** Carries the response plus whether a new transaction was created (201) or replayed (200). */
public record PostingOutcome(TransactionResponse response, boolean created) { }
