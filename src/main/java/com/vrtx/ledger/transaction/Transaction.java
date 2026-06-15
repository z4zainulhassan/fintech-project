package com.vrtx.ledger.transaction;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false)
    private String idempotencyKey;

    /** Transaction linking: a refund points at the payment it reverses. */
    @Column(name = "related_transaction_id", updatable = false)
    private Long relatedTransactionId;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() { }

    public Transaction(TransactionType type, String idempotencyKey, Long relatedTransactionId,
                       String currency, long amount) {
        this.type = type;
        this.status = TransactionStatus.COMPLETED;
        this.idempotencyKey = idempotencyKey;
        this.relatedTransactionId = relatedTransactionId;
        this.currency = currency;
        this.amount = amount;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getRelatedTransactionId() { return relatedTransactionId; }
    public String getCurrency() { return currency; }
    public long getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
