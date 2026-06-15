package com.vrtx.ledger.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.vrtx.ledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Test
    void payment_splits_into_merchant_and_fees_and_balances_move() {
        long user1Before = balance(1);
        long merchantBefore = balance(2);
        long feesBefore = balance(3);

        String body = """
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":10000,"feeAmount":300,"currency":"PKR"}""";

        ResponseEntity<JsonNode> res = post("/transactions/payment", body, newKey());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode b = res.getBody();
        assertThat(b.get("type").asText()).isEqualTo("PAYMENT");
        assertThat(b.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(b.get("entries")).hasSize(3);

        // Double-entry moved correctly.
        assertThat(balance(1)).isEqualTo(user1Before - 10000);
        assertThat(balance(2)).isEqualTo(merchantBefore + 9700);
        assertThat(balance(3)).isEqualTo(feesBefore + 300);
    }

    @Test
    void payment_with_default_fee_rate_when_feeAmount_omitted() {
        // 3% of 20000 = 600
        long merchantBefore = balance(2);
        long feesBefore = balance(3);
        String body = """
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":20000,"currency":"PKR"}""";

        ResponseEntity<JsonNode> res = post("/transactions/payment", body, newKey());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(balance(2)).isEqualTo(merchantBefore + 19400);
        assertThat(balance(3)).isEqualTo(feesBefore + 600);
    }

    @Test
    void payment_exceeding_balance_is_rejected_422() {
        String body = """
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":200000000,"feeAmount":0,"currency":"PKR"}""";

        ResponseEntity<JsonNode> res = post("/transactions/payment", body, newKey());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody().get("error").asText()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void payment_currency_mismatch_is_rejected_422() {
        // account 6 is USD; sending PKR must fail.
        String body = """
                {"userAccountId":1,"merchantAccountId":6,"feesAccountId":3,
                 "amount":10000,"feeAmount":0,"currency":"PKR"}""";

        ResponseEntity<JsonNode> res = post("/transactions/payment", body, newKey());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody().get("error").asText()).isEqualTo("CURRENCY_MISMATCH");
    }

    @Test
    void payment_missing_idempotency_key_is_400() {
        String body = """
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":10000,"feeAmount":0,"currency":"PKR"}""";

        ResponseEntity<JsonNode> res = post("/transactions/payment", body, null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().get("error").asText()).isEqualTo("VALIDATION_ERROR");
    }
}
