package com.vrtx.ledger.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.vrtx.ledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerInvariantsTest extends AbstractIntegrationTest {

    @Test
    void settlement_sweeps_merchant_balance_to_settlement_account() {
        // Fund the merchant via a payment first.
        post("/transactions/payment", """
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":50000,"feeAmount":0,"currency":"PKR"}""", newKey());

        long merchantBefore = balance(2);
        long settlementBefore = balance(5);

        ResponseEntity<JsonNode> res = post("/transactions/settlement", """
                {"merchantAccountId":2,"settlementAccountId":5,"amount":50000,"currency":"PKR"}""", newKey());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(balance(2)).isEqualTo(merchantBefore - 50000);
        assertThat(balance(5)).isEqualTo(settlementBefore + 50000);
    }

    @Test
    void ledger_is_globally_balanced_after_mixed_operations() {
        post("/transactions/payment", """
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":12345,"feeAmount":345,"currency":"PKR"}""", newKey());
        post("/transactions/transfer", """
                {"fromAccountId":1,"toAccountId":4,"amount":7777,"currency":"PKR"}""", newKey());
        ResponseEntity<JsonNode> pay = post("/transactions/payment", """
                {"userAccountId":4,"merchantAccountId":2,"feesAccountId":3,
                 "amount":9000,"feeAmount":270,"currency":"PKR"}""", newKey());
        long payId = pay.getBody().get("transactionId").asLong();
        post("/transactions/refund",
                String.format("{\"originalTransactionId\":%d,\"amount\":3000}", payId), newKey());

        JsonNode recon = get("/reconciliation").getBody();
        assertThat(recon.get("balanced").asBoolean()).isTrue();
        assertThat(recon.get("totalDebits").asLong()).isEqualTo(recon.get("totalCredits").asLong());
        assertThat(recon.get("unbalancedTransactionCount").asInt()).isZero();
    }
}
