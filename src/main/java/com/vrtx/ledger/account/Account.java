package com.vrtx.ledger.account;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() { }

    public Account(AccountType type, String currency) {
        this.type = type;
        this.currency = currency;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public AccountType getType() { return type; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }

    /** USER and MERCHANT wallets represent real balances and must not go negative.
     *  FEES / SETTLEMENT are system/boundary accounts and may go negative. */
    public boolean isBalanceProtected() {
        return type == AccountType.USER || type == AccountType.MERCHANT;
    }
}
