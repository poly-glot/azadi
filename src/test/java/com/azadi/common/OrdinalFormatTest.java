package com.azadi.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrdinalFormatTest {

    @ParameterizedTest(name = "day {0} → \"{1}\"")
    @CsvSource({
        "1,  1st",
        "2,  2nd",
        "3,  3rd",
        "4,  4th",
        "10, 10th",
        "11, 11th",
        "12, 12th",
        "13, 13th",
        "14, 14th",
        "21, 21st",
        "22, 22nd",
        "23, 23rd",
        "28, 28th"
    })
    @DisplayName("dayWithSuffix produces correct ordinal")
    void dayWithSuffix_producesCorrectOrdinal(int day, String expected) {
        assertThat(OrdinalFormat.dayWithSuffix(day)).isEqualTo(expected);
    }
}
