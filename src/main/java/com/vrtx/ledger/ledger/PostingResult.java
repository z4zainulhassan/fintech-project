package com.vrtx.ledger.ledger;

import com.vrtx.ledger.transaction.Transaction;

/**
 * Outcome of a posting attempt.
 * created == true  -> a new transaction was written (HTTP 201)
 * created == false -> idempotency replay, existing transaction returned (HTTP 200)
 */
public record PostingResult(Transaction transaction, boolean created) { }
