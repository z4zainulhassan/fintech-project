package com.vrtx.ledger.ledger;

import com.vrtx.ledger.account.Account;
import com.vrtx.ledger.transaction.Transaction;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6, updatable = false)
    private EntryDirection direction;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() { }

    public LedgerEntry(Transaction transaction, Account account, EntryDirection direction,
                       long amount, String currency) {
        this.transaction = transaction;
        this.account = account;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public Account getAccount() { return account; }
    public EntryDirection getDirection() { return direction; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
