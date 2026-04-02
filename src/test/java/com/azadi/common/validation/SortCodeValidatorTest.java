package com.azadi.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SortCodeValidatorTest {

    private SortCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SortCodeValidator();
    }

    @Test
    @DisplayName("Should accept valid sort code with dashes")
    void validSortCode() {
        assertThat(validator.isValid("12-34-56", null)).isTrue();
    }

    @Test
    @DisplayName("Should reject sort code without dashes")
    void rejectSortCodeWithoutDashes() {
        assertThat(validator.isValid("123456", null)).isFalse();
    }

    @Test
    @DisplayName("Should reject incomplete sort code")
    void rejectIncompleteSortCode() {
        assertThat(validator.isValid("12-34", null)).isFalse();
    }

    @Test
    @DisplayName("Should reject alphabetic sort code")
    void rejectAlphabeticSortCode() {
        assertThat(validator.isValid("ab-cd-ef", null)).isFalse();
    }

    @Test
    @DisplayName("Should accept null input (Bean Validation convention - @NotBlank handles null)")
    void acceptNullInput() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("Should accept empty input (Bean Validation convention - @NotBlank handles empty)")
    void acceptEmptyInput() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    @DisplayName("Should reject sort code with extra digits")
    void rejectSortCodeWithExtraDigits() {
        assertThat(validator.isValid("12-34-567", null)).isFalse();
    }
}
