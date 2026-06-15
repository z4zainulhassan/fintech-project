package com.vrtx.ledger.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to committed transactions. Fires only AFTER_COMMIT so downstream side
 * effects (notifications, settlement triggers, analytics) never run for a rolled-back txn.
 * Replace the log with a real publisher (Kafka/SNS) to fan out events.
 */
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TransactionPostedEvent event) {
        log.info("transaction.posted id={} type={} amount={} {}",
                event.transactionId(), event.type(), event.amount(), event.currency());
    }
}
