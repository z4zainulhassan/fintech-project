package com.vrtx.ledger.transaction.service;

import com.vrtx.ledger.account.Account;
import com.vrtx.ledger.account.AccountRepository;
import com.vrtx.ledger.account.AccountType;
import com.vrtx.ledger.exception.RefundNotAllowedException;
import com.vrtx.ledger.exception.TransactionNotFoundException;
import com.vrtx.ledger.ledger.*;
import com.vrtx.ledger.transaction.Transaction;
import com.vrtx.ledger.transaction.TransactionRepository;
import com.vrtx.ledger.transaction.TransactionType;
import com.vrtx.ledger.transaction.dto.PostingOutcome;
import com.vrtx.ledger.transaction.dto.RefundRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Refund / reversal of a PAYMENT. Supports partial refunds and is linked to the
 * original via related_transaction_id (transaction linking).
 *
 * For a refund of r against a payment of principal P that originally split into
 * merchant m and fees f (m + f = P), we reverse proportionally:
 *   feeShare      = floor(r * f / P)
 *   merchantShare = r - feeShare
 *   Credit User     r
 *   Debit  Merchant merchantShare
 *   Debit  Fees     feeShare
 * Balanced by construction (credit r == debit merchantShare + feeShare).
 *
 * Concurrency: this method is transactional and locks the user/merchant/fees
 * accounts before reading the cumulative refunded amount, so two concurrent
 * partial refunds of the same payment cannot jointly exceed the principal.
 */
@Service
public class RefundService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;
    private final LedgerService ledger;
    private final TransactionResponses responses;

    public RefundService(TransactionRepository transactionRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         AccountRepository accountRepository,
                         LedgerService ledger,
                         TransactionResponses responses) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
        this.ledger = ledger;
        this.responses = responses;
    }

    @Transactional
    public PostingOutcome refund(RefundRequest req, String idempotencyKey) {
        // Idempotency replay (sequential): return the original refund untouched.
        var replay = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            return new PostingOutcome(responses.toResponse(replay.get()), false);
        }

        Transaction original = transactionRepository.findById(req.originalTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException(req.originalTransactionId()));

        if (original.getType() != TransactionType.PAYMENT) {
            throw new RefundNotAllowedException("Only PAYMENT transactions can be refunded");
        }

        long principal = original.getAmount();
        long refundAmount = req.amount();

        // Resolve the original legs.
        List<LedgerEntry> originalEntries = ledgerEntryRepository
                .findByTransactionIdOrderByIdAsc(original.getId());

        Long userAccountId = null;
        Long merchantAccountId = null;
        Long feesAccountId = null;
        long originalFee = 0;

        for (LedgerEntry e : originalEntries) {
            Account acc = e.getAccount();
            if (e.getDirection() == EntryDirection.DEBIT) {
                userAccountId = acc.getId();                 // the payer
            } else if (acc.getType() == AccountType.FEES) {
                feesAccountId = acc.getId();
                originalFee = e.getAmount();
            } else {
                merchantAccountId = acc.getId();             // the payee
            }
        }
        if (userAccountId == null || merchantAccountId == null) {
            throw new RefundNotAllowedException("Original payment legs could not be resolved");
        }

        // Lock involved accounts BEFORE reading cumulative refunds (serialises concurrent refunds).
        List<Long> toLock = new ArrayList<>();
        toLock.add(userAccountId);
        toLock.add(merchantAccountId);
        if (feesAccountId != null) toLock.add(feesAccountId);
        accountRepository.lockByIds(toLock.stream().distinct().sorted().toList());

        // Cap cumulative refunds at the principal.
        long alreadyRefunded = transactionRepository
                .findByRelatedTransactionIdAndType(original.getId(), TransactionType.REFUND)
                .stream().mapToLong(Transaction::getAmount).sum();
        long remaining = principal - alreadyRefunded;
        if (refundAmount > remaining) {
            throw new RefundNotAllowedException(
                    "Refund " + refundAmount + " exceeds remaining refundable " + remaining);
        }

        // Proportional split of the refund between merchant and fees.
        long feeShare = principal == 0 ? 0 : Math.floorDiv(refundAmount * originalFee, principal);
        long merchantShare = refundAmount - feeShare;

        List<PostingLeg> legs = new ArrayList<>(3);
        legs.add(PostingLeg.credit(userAccountId, refundAmount));
        if (merchantShare > 0) {
            // Not balance-protected: a refund may legitimately push a settled merchant negative.
            legs.add(PostingLeg.debit(merchantAccountId, merchantShare, false));
        }
        if (feeShare > 0 && feesAccountId != null) {
            legs.add(PostingLeg.debit(feesAccountId, feeShare, false));
        }

        PostingResult result = ledger.post(
                TransactionType.REFUND, original.getCurrency(), refundAmount,
                original.getId(), idempotencyKey, legs);

        return responses.outcome(result);
    }
}
