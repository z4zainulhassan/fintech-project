package com.vrtx.ledger.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.vrtx.ledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyTest extends AbstractIntegrationTest {

    @Test
    void same_key_replays_original_and_applies_effect_once() {
        String key = newKey();
        String body = """
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":10000,"feeAmount":300,"currency":"PKR"}""";

        long merchantBefore = balance(2);

        ResponseEntity<JsonNode> first = post("/transactions/payment", body, key);
        ResponseEntity<JsonNode> second = post("/transactions/payment", body, key);

        // First creates (201), replay returns original (200).
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Same transaction id both times.
        assertThat(second.getBody().get("transactionId").asLong())
                .isEqualTo(first.getBody().get("transactionId").asLong());

        // Effect applied exactly once.
        assertThat(balance(2)).isEqualTo(merchantBefore + 9700);
    }
}
