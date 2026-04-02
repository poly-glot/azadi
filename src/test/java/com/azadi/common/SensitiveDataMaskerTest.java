package com.azadi.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    @DisplayName("maskSortCode masks first two groups")
    void maskSortCode() {
        assertThat(SensitiveDataMasker.maskSortCode("12-34-56")).isEqualTo("**-**-56");
    }

    @Test
    @DisplayName("maskAccountNumber masks first four digits")
    void maskAccountNumber() {
        assertThat(SensitiveDataMasker.maskAccountNumber("12345678")).isEqualTo("****5678");
    }

    @Test
    @DisplayName("maskSortCode returns fully masked for null input")
    void maskSortCodeNullInput() {
        assertThat(SensitiveDataMasker.maskSortCode(null)).isEqualTo("**-**-**");
    }

    @Test
    @DisplayName("maskAccountNumber returns fully masked for null input")
    void maskAccountNumberNullInput() {
        assertThat(SensitiveDataMasker.maskAccountNumber(null)).isEqualTo("****");
    }

    @Test
    @DisplayName("maskSortCode handles already masked input")
    void maskSortCodeAlreadyMasked() {
        // Should still produce a valid masked output even if input is odd
        String result = SensitiveDataMasker.maskSortCode("**-**-56");
        assertThat(result).isEqualTo("**-**-56");
    }

    @Test
    @DisplayName("maskAccountNumber handles short input gracefully")
    void maskAccountNumberShortInput() {
        // For very short account numbers, mask what is available
        String result = SensitiveDataMasker.maskAccountNumber("1234");
        assertThat(result).isNotNull();
    }
}
