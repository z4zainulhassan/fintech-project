package com.vrtx.ledger.transaction.service;

import com.vrtx.ledger.ledger.LedgerEntry;
import com.vrtx.ledger.ledger.LedgerEntryRepository;
import com.vrtx.ledger.ledger.PostingResult;
import com.vrtx.ledger.transaction.Transaction;
import com.vrtx.ledger.transaction.dto.PostingOutcome;
import com.vrtx.ledger.transaction.dto.TransactionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/** Builds API responses from persisted transactions (single place, no duplication). */
@Component
public class TransactionResponses {

    private final LedgerEntryRepository entryRepository;

    public TransactionResponses(LedgerEntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    public PostingOutcome outcome(PostingResult result) {
        return new PostingOutcome(toResponse(result.transaction()), result.created());
    }

    public TransactionResponse toResponse(Transaction txn) {
        List<LedgerEntry> entries = entryRepository.findByTransactionIdOrderByIdAsc(txn.getId());
        return TransactionResponse.from(txn, entries);
    }
}
