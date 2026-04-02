package com.azadi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class UkPostcodeValidator implements ConstraintValidator<UkPostcode, String> {

    private static final Pattern UK_POSTCODE = Pattern.compile(
        "^[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2}$",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return UK_POSTCODE.matcher(value.trim()).matches();
    }
}
