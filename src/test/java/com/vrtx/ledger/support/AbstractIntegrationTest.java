package com.vrtx.ledger.support;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final long OPENING_BALANCE = 100_000_000L;

    @Autowired protected TestRestTemplate rest;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetDatabase() {
        // wipe mutable data between tests, keep account rows (Flyway seeded them)
        jdbc.execute("TRUNCATE ledger_entries RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE transactions RESTART IDENTITY CASCADE");

        // re-seed opening balances for user 1
        jdbc.execute("""
            WITH t AS (
                INSERT INTO transactions(type,status,idempotency_key,currency,amount)
                VALUES ('TRANSFER','COMPLETED','seed-open-user-1','PKR',100000000)
                RETURNING id
            )
            INSERT INTO ledger_entries(transaction_id,account_id,direction,amount,currency)
            SELECT id,5,'DEBIT',100000000,'PKR' FROM t
            UNION ALL
            SELECT id,1,'CREDIT',100000000,'PKR' FROM t
            """);

        // re-seed opening balances for user 4
        jdbc.execute("""
            WITH t AS (
                INSERT INTO transactions(type,status,idempotency_key,currency,amount)
                VALUES ('TRANSFER','COMPLETED','seed-open-user-4','PKR',100000000)
                RETURNING id
            )
            INSERT INTO ledger_entries(transaction_id,account_id,direction,amount,currency)
            SELECT id,5,'DEBIT',100000000,'PKR' FROM t
            UNION ALL
            SELECT id,4,'CREDIT',100000000,'PKR' FROM t
            """);
    }

    protected static String newKey() {
        return UUID.randomUUID().toString();
    }

    protected HttpHeaders jsonHeaders(String idempotencyKey) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) h.set("Idempotency-Key", idempotencyKey);
        return h;
    }

    protected ResponseEntity<JsonNode> post(String path, String body, String key) {
        return rest.exchange(path, HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(key)), JsonNode.class);
    }

    protected ResponseEntity<JsonNode> get(String path) {
        return rest.getForEntity(path, JsonNode.class);
    }

    protected long balance(long accountId) {
        return get("/accounts/" + accountId + "/ledger")
                .getBody().get("balance").asLong();
    }
}