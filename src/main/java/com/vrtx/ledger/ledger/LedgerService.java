package com.vrtx.ledger.ledger;

import com.vrtx.ledger.account.Account;
import com.vrtx.ledger.account.AccountRepository;
import com.vrtx.ledger.event.TransactionPostedEvent;
import com.vrtx.ledger.exception.*;
import com.vrtx.ledger.transaction.Transaction;
import com.vrtx.ledger.transaction.TransactionRepository;
import com.vrtx.ledger.transaction.TransactionType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * The single place where any money movement is validated and persisted.
 * Every scenario (payment/refund/transfer/settlement) reduces to a list of
 * PostingLegs handed to {@link #post}. Centralising this guarantees the same
 * invariants hold everywhere: balanced, positive, single-currency, no overdraft,
 * atomic, append-only, idempotent.
 */
@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher events;

    public LedgerService(AccountRepository accountRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         TransactionRepository transactionRepository,
                         ApplicationEventPublisher events) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionRepository = transactionRepository;
        this.events = events;
    }

    /**
     * Post a balanced double-entry transaction atomically.
     *
     * @throws DuplicateIdempotencyKeyException only on the concurrent insert race;
     *         the caller recovers by re-reading the original. Sequential replays are
     *         handled here directly via the fast-path lookup below.
     */
    @Transactional
    public PostingResult post(TransactionType type,
                              String currency,
                              long amount,
                              Long relatedTransactionId,
                              String idempotencyKey,
                              List<PostingLeg> legs) {

        // --- Fast path: sequential idempotency replay (most duplicates) ---
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new PostingResult(existing.get(), false);
        }

        if (legs == null || legs.size() < 2) {
            throw new IllegalArgumentException("a transaction needs at least two legs");
        }

        // --- Lock all involved accounts FOR UPDATE, in id order (deadlock-safe) ---
        List<Long> accountIds = legs.stream().map(PostingLeg::accountId).distinct().sorted().toList();
        List<Account> locked = accountRepository.lockByIds(accountIds);
        if (locked.size() != accountIds.size()) {
            Set<Long> found = new HashSet<>();
            locked.forEach(a -> found.add(a.getId()));
            Long missing = accountIds.stream().filter(id -> !found.contains(id)).findFirst().orElse(null);
            throw new AccountNotFoundException(missing);
        }
        Map<Long, Account> accounts = new HashMap<>();
        locked.forEach(a -> accounts.put(a.getId(), a));

        // --- Currency consistency: all legs + their accounts share one currency (no FX) ---
        for (PostingLeg leg : legs) {
            Account acc = accounts.get(leg.accountId());
            if (!acc.getCurrency().equals(currency)) {
                throw new CurrencyMismatchException(
                        "Account " + acc.getId() + " is " + acc.getCurrency() + ", transaction is " + currency);
            }
        }

        // --- Balanced invariant: sum(debits) == sum(credits) ---
        long debits = legs.stream().filter(l -> l.direction() == EntryDirection.DEBIT)
                .mapToLong(PostingLeg::amount).sum();
        long credits = legs.stream().filter(l -> l.direction() == EntryDirection.CREDIT)
                .mapToLong(PostingLeg::amount).sum();
        if (debits != credits) {
            throw new UnbalancedTransactionException(debits, credits);
        }

        // --- Overdraft check on protected accounts, using net effect of this txn ---
        Map<Long, Long> netDelta = new HashMap<>();   // accountId -> credit(+)/debit(-)
        Set<Long> protectedAccounts = new HashSet<>();
        for (PostingLeg leg : legs) {
            long signed = leg.direction() == EntryDirection.CREDIT ? leg.amount() : -leg.amount();
            netDelta.merge(leg.accountId(), signed, Long::sum);
            if (leg.balanceProtected()) protectedAccounts.add(leg.accountId());
        }
        for (Long accId : protectedAccounts) {
            long projected = ledgerEntryRepository.balanceOf(accId) + netDelta.getOrDefault(accId, 0L);
            if (projected < 0) {
                throw new InsufficientFundsException(accId);
            }
        }

        // --- Persist transaction; flush now so the UNIQUE key violation surfaces here ---
        Transaction txn = new Transaction(type, idempotencyKey, relatedTransactionId, currency, amount);
        try {
            txn = transactionRepository.saveAndFlush(txn);
        } catch (DataIntegrityViolationException e) {
            // Concurrent request with the same key won the race and committed first.
            throw new DuplicateIdempotencyKeyException(idempotencyKey);
        }

        // --- Append immutable ledger entries ---
        List<LedgerEntry> entries = new ArrayList<>(legs.size());
        for (PostingLeg leg : legs) {
            entries.add(new LedgerEntry(txn, accounts.get(leg.accountId()),
                    leg.direction(), leg.amount(), currency));
        }
        ledgerEntryRepository.saveAll(entries);

        // --- Fire domain event after commit (bonus: event-driven) ---
        events.publishEvent(new TransactionPostedEvent(txn.getId(), type, currency, amount));

        return new PostingResult(txn, true);
    }
}
