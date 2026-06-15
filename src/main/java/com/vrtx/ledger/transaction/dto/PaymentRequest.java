package com.vrtx.ledger.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** feeAmount is optional: if null the configured fee rate is applied (bonus: configurable fees). */
public record PaymentRequest(
        @NotNull Long userAccountId,
        @NotNull Long merchantAccountId,
        @NotNull Long feesAccountId,
        @NotNull @Positive Long amount,
        Long feeAmount,
        @NotBlank @Size(min = 3, max = 3) String currency
) { }
