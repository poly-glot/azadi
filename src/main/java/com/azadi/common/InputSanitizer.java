package com.azadi.common;

public final class InputSanitizer {

    private InputSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "").trim();
    }

    public static String stripAll(String input) {
        if (input == null) {
            return null;
        }
        // Strip all HTML tags completely
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
