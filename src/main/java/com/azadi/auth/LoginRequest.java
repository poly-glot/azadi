package com.azadi.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Date of birth is required")
    String dob,

    @NotBlank(message = "Postcode is required")
    String postcode,

    @NotBlank(message = "Agreement number is required")
    String agreementNumber
) {
}
