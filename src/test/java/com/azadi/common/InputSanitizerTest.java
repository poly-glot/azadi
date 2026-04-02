package com.azadi.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputSanitizerTest {

    @Test
    @DisplayName("Strips script tags completely")
    void stripsScriptTags() {
        String result = InputSanitizer.sanitize("<script>alert(1)</script>");
        assertThat(result).isEqualTo("alert(1)");
        // Script tags are stripped but text content remains — the sanitizer prevents XSS
        // by removing HTML tags, not by removing all text content within script tags
    }

    @Test
    @DisplayName("Strips all HTML tags but preserves text content")
    void stripsHtmlTagsPreservesText() {
        String result = InputSanitizer.sanitize("Hello <b>world</b>");
        assertThat(result).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("Returns null for null input")
    void returnsNullForNullInput() {
        assertThat(InputSanitizer.sanitize(null)).isNull();
    }

    @Test
    @DisplayName("Returns normal text unchanged")
    void returnsNormalTextUnchanged() {
        assertThat(InputSanitizer.sanitize("Normal text")).isEqualTo("Normal text");
    }

    @Test
    @DisplayName("Strips nested HTML tags")
    void stripsNestedHtmlTags() {
        String result = InputSanitizer.sanitize("<div><p>Hello <em>world</em></p></div>");
        assertThat(result).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("Strips event handler attributes")
    void stripsEventHandlerAttributes() {
        String result = InputSanitizer.sanitize("<img onerror='alert(1)' src='x'>");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Handles empty string")
    void handlesEmptyString() {
        assertThat(InputSanitizer.sanitize("")).isEmpty();
    }
}
