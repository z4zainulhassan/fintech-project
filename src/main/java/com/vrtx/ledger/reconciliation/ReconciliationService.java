package com.vrtx.ledger.reconciliation;

import com.vrtx.ledger.ledger.LedgerEntryRepository;
import com.vrtx.ledger.reconciliation.dto.ReconciliationReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reconciliation: proves the ledger is internally consistent.
 *   1) Every transaction balances (debits == credits).
 *   2) Globally, sum(debits) == sum(credits)  -> no money created or destroyed.
 */
@Service
public class ReconciliationService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public ReconciliationService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationReport run() {
        long debits = ledgerEntryRepository.totalDebits();
        long credits = ledgerEntryRepository.totalCredits();

        List<ReconciliationReport.UnbalancedTransactionDto> unbalanced =
                ledgerEntryRepository.findUnbalancedTransactions().stream()
                        .map(u -> new ReconciliationReport.UnbalancedTransactionDto(
                                u.getTransactionId(), u.getDebits(), u.getCredits()))
                        .toList();

        boolean balanced = debits == credits && unbalanced.isEmpty();
        return new ReconciliationReport(balanced, debits, credits, debits - credits,
                unbalanced.size(), unbalanced);
    }
}
