package com.azadi.common;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public final class InputSanitizer {

    private static final PolicyFactory STRIP_ALL_POLICY = new HtmlPolicyBuilder().toFactory();

    private InputSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return STRIP_ALL_POLICY.sanitize(input).trim();
    }

    public static String stripAll(String input) {
        if (input == null) {
            return null;
        }
        return STRIP_ALL_POLICY.sanitize(input).trim();
    }
}
