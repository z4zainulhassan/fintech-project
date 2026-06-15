package com.vrtx.ledger.ledger;

/**
 * One side of a double-entry posting: "move {amount} {direction} on {accountId}".
 * Services describe transactions purely as a list of legs; LedgerService enforces
 * the accounting rules (balanced, positive, single-currency) and persists them.
 *
 * @param balanceProtected if true, this account must not end up negative after the
 *                         posting (used for the debited wallet to detect insufficient funds).
 */
public record PostingLeg(Long accountId, EntryDirection direction, long amount, boolean balanceProtected) {

    public PostingLeg {
        if (accountId == null) throw new IllegalArgumentException("accountId required");
        if (direction == null) throw new IllegalArgumentException("direction required");
        if (amount <= 0) throw new IllegalArgumentException("leg amount must be > 0");
    }

    public static PostingLeg debit(Long accountId, long amount, boolean protect) {
        return new PostingLeg(accountId, EntryDirection.DEBIT, amount, protect);
    }

    public static PostingLeg credit(Long accountId, long amount) {
        return new PostingLeg(accountId, EntryDirection.CREDIT, amount, false);
    }
}
