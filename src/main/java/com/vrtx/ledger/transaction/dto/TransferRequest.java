package com.vrtx.ledger.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferRequest(
        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,
        @NotNull @Positive Long amount,
        @NotBlank @Size(min = 3, max = 3) String currency
) { }
