package com.azadi.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UkPostcodeValidatorTest {

    private UkPostcodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UkPostcodeValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SW1A 1AA", "M1 1AE", "B1 1BB", "LS1 1BA", "EH1 1YZ"})
    @DisplayName("Should accept valid UK postcodes")
    void validPostcodes(String postcode) {
        assertThat(validator.isValid(postcode, null)).isTrue();
    }

    @Test
    @DisplayName("Should accept empty string (Bean Validation convention - @NotBlank handles empty)")
    void acceptEmptyString() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    @DisplayName("Should reject numeric-only input")
    void rejectNumericInput() {
        assertThat(validator.isValid("12345", null)).isFalse();
    }

    @Test
    @DisplayName("Should reject clearly invalid postcode")
    void rejectInvalidPostcode() {
        assertThat(validator.isValid("INVALID", null)).isFalse();
    }

    @Test
    @DisplayName("Should accept null input (Bean Validation convention)")
    void handleNullInput() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("Should accept lowercase postcodes")
    void acceptLowercasePostcodes() {
        assertThat(validator.isValid("sw1a 1aa", null)).isTrue();
    }

    @Test
    @DisplayName("Should accept postcodes without space")
    void acceptPostcodesWithoutSpace() {
        assertThat(validator.isValid("SW1A1AA", null)).isTrue();
    }
}
