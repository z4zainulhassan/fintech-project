package com.vrtx.ledger.concurrency;

import com.fasterxml.jackson.databind.JsonNode;
import com.vrtx.ledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyTest extends AbstractIntegrationTest {

    /** Parallel payments with distinct keys: no lost updates, ledger stays balanced. */
    @Test
    void parallel_distinct_payments_have_no_lost_updates() throws Exception {
        int threads = 20;
        long amount = 1000;
        long user1Before = balance(1);
        long merchantBefore = balance(2);

        runConcurrently(threads, i -> {
            String body = """
                    {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                     "amount":1000,"feeAmount":0,"currency":"PKR"}""";
            ResponseEntity<JsonNode> res = post("/transactions/payment", body, newKey());
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        });

        assertThat(balance(1)).isEqualTo(user1Before - threads * amount);
        assertThat(balance(2)).isEqualTo(merchantBefore + threads * amount);

        // Global reconciliation must be balanced.
        JsonNode recon = get("/reconciliation").getBody();
        assertThat(recon.get("balanced").asBoolean()).isTrue();
        assertThat(recon.get("difference").asLong()).isZero();
    }

    /** Same idempotency key fired in parallel: exactly one transaction is created. */
    @Test
    void same_key_in_parallel_creates_exactly_one_transaction() throws Exception {
        int threads = 20;
        String key = newKey();
        String body = """
                {"userAccountId":1,"merchantAccountId":2,"feesAccountId":3,
                 "amount":1000,"feeAmount":0,"currency":"PKR"}""";
        long merchantBefore = balance(2);

        Set<Long> ids = Collections.synchronizedSet(new HashSet<>());
        runConcurrently(threads, i -> {
            ResponseEntity<JsonNode> res = post("/transactions/payment", body, key);
            assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
            ids.add(res.getBody().get("transactionId").asLong());
        });

        // All responses point at the same single transaction; effect applied once.
        assertThat(ids).hasSize(1);
        assertThat(balance(2)).isEqualTo(merchantBefore + 1000);
    }

    /** Overdraft race: concurrent payments summing beyond balance never drive it negative. */
    @Test
    void overdraft_race_never_goes_negative() throws Exception {
        // Drain user 4 down to exactly 10000 (100.00).
        long u4 = balance(4);
        long drain = u4 - 10000;
        ResponseEntity<JsonNode> t = post("/transactions/transfer",
                String.format("{\"fromAccountId\":4,\"toAccountId\":1,\"amount\":%d,\"currency\":\"PKR\"}", drain),
                newKey());
        assertThat(t.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(balance(4)).isEqualTo(10000);

        // Fire 20 concurrent payments of 1000 each (max 10 can succeed).
        int threads = 20;
        AtomicInteger created = new AtomicInteger();
        runConcurrently(threads, i -> {
            String body = """
                    {"userAccountId":4,"merchantAccountId":2,"feesAccountId":3,
                     "amount":1000,"feeAmount":0,"currency":"PKR"}""";
            ResponseEntity<JsonNode> res = post("/transactions/payment", body, newKey());
            if (res.getStatusCode() == HttpStatus.CREATED) created.incrementAndGet();
            else assertThat(res.getBody().get("error").asText()).isEqualTo("INSUFFICIENT_FUNDS");
        });

        assertThat(balance(4)).isGreaterThanOrEqualTo(0);
        assertThat(created.get()).isLessThanOrEqualTo(10);
        assertThat(balance(4)).isEqualTo(10000 - created.get() * 1000L);
    }

    private void runConcurrently(int threads, ThreadTask task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run(idx);
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();          // release all threads at once
        done.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();
        if (!errors.isEmpty()) throw new AssertionError("concurrent task failed", errors.peek());
    }

    @FunctionalInterface
    private interface ThreadTask { void run(int index); }
}
