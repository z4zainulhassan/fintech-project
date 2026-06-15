package com.vrtx.ledger.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Partial refunds supported: amount may be <= remaining refundable amount. */
public record RefundRequest(
        @NotNull Long originalTransactionId,
        @NotNull @Positive Long amount
) { }
