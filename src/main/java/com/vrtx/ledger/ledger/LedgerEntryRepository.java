package com.vrtx.ledger.ledger;

import com.vrtx.ledger.reconciliation.UnbalancedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /** Ledger-based balance: credits add, debits subtract. Source of truth for balance. */
    @Query("""
           select coalesce(sum(case when e.direction = com.vrtx.ledger.ledger.EntryDirection.CREDIT
                                     then e.amount else -e.amount end), 0)
           from LedgerEntry e
           where e.account.id = :accountId
           """)
    long balanceOf(@Param("accountId") Long accountId);

    List<LedgerEntry> findByTransactionIdOrderByIdAsc(Long transactionId);

    List<LedgerEntry> findByAccountIdOrderByIdAsc(Long accountId);

    // ---- Reconciliation aggregates (native: simple, reliable enum-free SQL) ----

    @Query(value = "select coalesce(sum(amount),0) from ledger_entries where direction = 'DEBIT'",
            nativeQuery = true)
    long totalDebits();

    @Query(value = "select coalesce(sum(amount),0) from ledger_entries where direction = 'CREDIT'",
            nativeQuery = true)
    long totalCredits();

    /** Any transaction whose own debits != credits. Must always be empty in a healthy ledger. */
    @Query(value = """
           select transaction_id as transactionId,
                  sum(case when direction = 'DEBIT'  then amount else 0 end) as debits,
                  sum(case when direction = 'CREDIT' then amount else 0 end) as credits
           from ledger_entries
           group by transaction_id
           having sum(case when direction = 'DEBIT'  then amount else 0 end)
                <> sum(case when direction = 'CREDIT' then amount else 0 end)
           """, nativeQuery = true)
    List<UnbalancedTransaction> findUnbalancedTransactions();
}
