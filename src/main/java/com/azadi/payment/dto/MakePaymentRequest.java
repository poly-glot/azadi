package com.azadi.payment.dto;

import jakarta.validation.constraints.Min;

public record MakePaymentRequest(
    @Min(value = 100, message = "Minimum payment is 1.00") long amountPence,
    Long agreementId
) {
}
