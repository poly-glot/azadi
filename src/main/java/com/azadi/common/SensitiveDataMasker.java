package com.azadi.common;

public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String maskSortCode(String sortCode) {
        if (sortCode == null || sortCode.length() < 2) {
            return "**-**-**";
        }
        return "**-**-" + sortCode.substring(sortCode.length() - 2);
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }
        var parts = email.split("@", 2);
        var local = parts[0];
        var masked = local.length() <= 2
            ? "*".repeat(local.length())
            : local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }
}
