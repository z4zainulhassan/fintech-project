package com.vrtx.ledger.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /** All refunds linked to a given payment. Used to cap cumulative refunded amount. */
    List<Transaction> findByRelatedTransactionIdAndType(Long relatedTransactionId, TransactionType type);
}
