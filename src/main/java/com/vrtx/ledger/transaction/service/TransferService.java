package com.vrtx.ledger.transaction.service;

import com.vrtx.ledger.ledger.LedgerService;
import com.vrtx.ledger.ledger.PostingLeg;
import com.vrtx.ledger.ledger.PostingResult;
import com.vrtx.ledger.transaction.TransactionType;
import com.vrtx.ledger.transaction.dto.PostingOutcome;
import com.vrtx.ledger.transaction.dto.TransferRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wallet transfer A -> B.
 *   Debit  From  amount   (balance-protected)
 *   Credit To    amount
 */
@Service
public class TransferService {

    private final LedgerService ledger;
    private final TransactionResponses responses;

    public TransferService(LedgerService ledger, TransactionResponses responses) {
        this.ledger = ledger;
        this.responses = responses;
    }

    public PostingOutcome transfer(TransferRequest req, String idempotencyKey) {
        if (req.fromAccountId().equals(req.toAccountId())) {
            throw new IllegalArgumentException("fromAccountId and toAccountId must differ");
        }
        List<PostingLeg> legs = List.of(
                PostingLeg.debit(req.fromAccountId(), req.amount(), true),
                PostingLeg.credit(req.toAccountId(), req.amount())
        );
        PostingResult result = ledger.post(
                TransactionType.TRANSFER, req.currency(), req.amount(), null, idempotencyKey, legs);
        return responses.outcome(result);
    }
}
