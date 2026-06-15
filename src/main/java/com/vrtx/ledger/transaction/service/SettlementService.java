package com.vrtx.ledger.transaction.service;

import com.vrtx.ledger.ledger.LedgerService;
import com.vrtx.ledger.ledger.PostingLeg;
import com.vrtx.ledger.ledger.PostingResult;
import com.vrtx.ledger.transaction.TransactionType;
import com.vrtx.ledger.transaction.dto.PostingOutcome;
import com.vrtx.ledger.transaction.dto.SettlementRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Merchant settlement: sweep a merchant's collected balance to a settlement account.
 *   Debit  Merchant    amount   (balance-protected: cannot settle more than held)
 *   Credit Settlement  amount
 */
@Service
public class SettlementService {

    private final LedgerService ledger;
    private final TransactionResponses responses;

    public SettlementService(LedgerService ledger, TransactionResponses responses) {
        this.ledger = ledger;
        this.responses = responses;
    }

    public PostingOutcome settle(SettlementRequest req, String idempotencyKey) {
        List<PostingLeg> legs = List.of(
                PostingLeg.debit(req.merchantAccountId(), req.amount(), true),
                PostingLeg.credit(req.settlementAccountId(), req.amount())
        );
        PostingResult result = ledger.post(
                TransactionType.SETTLEMENT, req.currency(), req.amount(), null, idempotencyKey, legs);
        return responses.outcome(result);
    }
}
