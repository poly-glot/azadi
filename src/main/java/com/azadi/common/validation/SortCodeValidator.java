package com.azadi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SortCodeValidator implements ConstraintValidator<SortCode, String> {

    private static final Pattern SORT_CODE = Pattern.compile("^\\d{2}-\\d{2}-\\d{2}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return SORT_CODE.matcher(value).matches();
    }
}
