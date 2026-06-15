package com.vrtx.ledger.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.vrtx.ledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class RefundIntegrationTest extends AbstractIntegrationTest {

    private long createPayment(long amount, long fee) {
        String body = String.format("""
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":%d,"feeAmount":%d,"currency":"PKR"}""", amount, fee);
        ResponseEntity<JsonNode> res = post("/transactions/payment", body, newKey());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return res.getBody().get("transactionId").asLong();
    }

    @Test
    void full_refund_reverses_payment_and_links_to_original() {
        long user1Before = balance(1);
        long paymentId = createPayment(10000, 300);

        String refund = String.format("""
                {"originalTransactionId":%d,"amount":10000}""", paymentId);
        ResponseEntity<JsonNode> res = post("/transactions/refund", refund, newKey());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode b = res.getBody();
        assertThat(b.get("type").asText()).isEqualTo("REFUND");
        assertThat(b.get("relatedTransactionId").asLong()).isEqualTo(paymentId);

        // Net effect of payment + full refund on the user is zero.
        assertThat(balance(1)).isEqualTo(user1Before);
    }

    @Test
    void partial_refunds_accumulate_and_cannot_exceed_principal() {
        long paymentId = createPayment(10000, 0);

        // First partial refund of 6000 -> OK
        ResponseEntity<JsonNode> r1 = post("/transactions/refund",
                String.format("{\"originalTransactionId\":%d,\"amount\":6000}", paymentId), newKey());
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second partial refund of 5000 -> exceeds remaining 4000 -> 422
        ResponseEntity<JsonNode> r2 = post("/transactions/refund",
                String.format("{\"originalTransactionId\":%d,\"amount\":5000}", paymentId), newKey());
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r2.getBody().get("error").asText()).isEqualTo("REFUND_NOT_ALLOWED");

        // Remaining 4000 -> OK
        ResponseEntity<JsonNode> r3 = post("/transactions/refund",
                String.format("{\"originalTransactionId\":%d,\"amount\":4000}", paymentId), newKey());
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void refunding_a_non_payment_is_rejected() {
        // Create a transfer, then try to refund it.
        ResponseEntity<JsonNode> transfer = post("/transactions/transfer",
                "{\"fromAccountId\":4,\"toAccountId\":1,\"amount\":5000,\"currency\":\"PKR\"}", newKey());
        long transferId = transfer.getBody().get("transactionId").asLong();

        ResponseEntity<JsonNode> res = post("/transactions/refund",
                String.format("{\"originalTransactionId\":%d,\"amount\":5000}", transferId), newKey());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody().get("error").asText()).isEqualTo("REFUND_NOT_ALLOWED");
    }
}
