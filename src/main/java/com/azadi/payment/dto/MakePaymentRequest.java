package com.azadi.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MakePaymentRequest(
    @Min(value = 100, message = "Minimum payment is 1.00") long amountPence,
    @NotNull(message = "Agreement ID is required") Long agreementId
) {
}
