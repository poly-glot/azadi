package com.azadi.common;

public final class MoneyFormatter {

    private MoneyFormatter() {
    }

    public static String formatPence(long pence) {
        return String.format("\u00A3%,.2f", pence / 100.0);
    }
}
