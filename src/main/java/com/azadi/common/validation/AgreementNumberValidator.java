package com.azadi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class AgreementNumberValidator implements ConstraintValidator<AgreementNumber, String> {

    private static final Pattern AGREEMENT_NUMBER = Pattern.compile("^AGR-\\d{6}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return AGREEMENT_NUMBER.matcher(value).matches();
    }
}
