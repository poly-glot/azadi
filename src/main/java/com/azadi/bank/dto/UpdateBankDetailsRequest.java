package com.azadi.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateBankDetailsRequest(
    @NotBlank(message = "Account holder name is required")
    String accountHolderName,

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{8}", message = "Account number must be 8 digits")
    String accountNumber,

    @NotBlank(message = "Sort code is required")
    @Pattern(regexp = "\\d{2}-\\d{2}-\\d{2}", message = "Sort code must be in XX-XX-XX format")
    String sortCode
) {
}
