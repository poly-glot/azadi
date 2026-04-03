package com.azadi.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBankDetailsRequest(
    @NotBlank(message = "Account holder name is required")
    @Size(max = 70, message = "Account holder name must not exceed 70 characters")
    String accountHolderName,

    @NotBlank(message = "Account number is required")
    @Size(min = 8, max = 8, message = "Account number must be exactly 8 digits")
    @Pattern(regexp = "\\d{8}", message = "Account number must be 8 digits")
    String accountNumber,

    @NotBlank(message = "Sort code is required")
    @Pattern(regexp = "\\d{2}-\\d{2}-\\d{2}", message = "Sort code must be in XX-XX-XX format")
    String sortCode
) {
}
