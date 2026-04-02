package com.azadi.contact.dto;

import com.azadi.common.validation.UkPostcode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateContactDetailsRequest(
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[0-9]{10}$", message = "Phone number must be a valid UK number")
    String phone,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    String email,

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 100, message = "Address line 1 must not exceed 100 characters")
    String addressLine1,

    @Size(max = 100, message = "Address line 2 must not exceed 100 characters")
    String addressLine2,

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must not exceed 50 characters")
    String city,

    @NotBlank(message = "Postcode is required")
    @UkPostcode
    String postcode
) {
}
