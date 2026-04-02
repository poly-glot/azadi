package com.azadi.common;

public final class OrdinalFormat {

    private OrdinalFormat() {}

    public static String dayWithSuffix(int day) {
        return day + suffix(day);
    }

    public static String suffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
