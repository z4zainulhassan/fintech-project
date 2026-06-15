package com.vrtx.ledger.transaction.service;

import com.vrtx.ledger.fee.FeeCalculator;
import com.vrtx.ledger.ledger.LedgerService;
import com.vrtx.ledger.ledger.PostingLeg;
import com.vrtx.ledger.ledger.PostingResult;
import com.vrtx.ledger.transaction.TransactionType;
import com.vrtx.ledger.transaction.dto.PaymentRequest;
import com.vrtx.ledger.transaction.dto.PostingOutcome;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Card payment. User pays {amount}; merchant receives {amount - fee}; fees account
 * receives {fee}. Example from spec: 100 -> merchant 97, fees 3.
 *   Debit  User     amount        (balance-protected -> insufficient funds check)
 *   Credit Merchant amount - fee
 *   Credit Fees     fee
 */
@Service
public class PaymentService {

    private final LedgerService ledger;
    private final FeeCalculator feeCalculator;
    private final TransactionResponses responses;

    public PaymentService(LedgerService ledger, FeeCalculator feeCalculator, TransactionResponses responses) {
        this.ledger = ledger;
        this.feeCalculator = feeCalculator;
        this.responses = responses;
    }

    public PostingOutcome pay(PaymentRequest req, String idempotencyKey) {
        long amount = req.amount();
        long fee = feeCalculator.resolveFee(amount, req.feeAmount());
        long merchantAmount = amount - fee;

        List<PostingLeg> legs = new ArrayList<>(3);
        legs.add(PostingLeg.debit(req.userAccountId(), amount, true));
        legs.add(PostingLeg.credit(req.merchantAccountId(), merchantAmount));
        if (fee > 0) {
            legs.add(PostingLeg.credit(req.feesAccountId(), fee));
        }

        PostingResult result = ledger.post(
                TransactionType.PAYMENT, req.currency(), amount, null, idempotencyKey, legs);
        return responses.outcome(result);
    }
}
