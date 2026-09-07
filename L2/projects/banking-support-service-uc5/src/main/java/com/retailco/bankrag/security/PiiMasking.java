package com.retailco.bankrag.security;

/**
 * Hides part of sensitive personal information (PII) before it's shown
 * anywhere. This is different from encryption — the real, full value
 * still exists in full elsewhere; this class only controls what gets
 * DISPLAYED. Every method here takes a full sensitive value (an account
 * number, mobile number, email, or government id) and returns a
 * partially-hidden version safe to show in logs, API responses, or a UI —
 * following the common "show only the last 4 digits" style banks and
 * card networks use.
 * <p>
 * {@code BankingToolService} calls these methods before ever returning
 * account, transaction, or loan data to a caller — the raw, unmasked data
 * never leaves that boundary.
 */
public final class PiiMasking {

    private PiiMasking() {
    }

    /** "1234567890123456" -> "XXXXXXXXXXXX3456" (keeps last 4 digits, per PCI-DSS-style display rules). */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "XXXX";
        }
        int visible = 4;
        return "X".repeat(accountNumber.length() - visible) + accountNumber.substring(accountNumber.length() - visible);
    }

    /** "9876543210" -> "XXXXXX3210" (mobile numbers: last 4 visible). */
    public static String maskMobileNumber(String mobile) {
        return maskAccountNumber(mobile);
    }

    /** "anaya.sharma@example.com" -> "an***@example.com". */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visiblePrefix = local.length() <= 2 ? local : local.substring(0, 2);
        return visiblePrefix + "***" + domain;
    }

    /** "ABCPD1234E" (PAN-style id) -> "ABCXXXXX4E" (first 3 and last 2 visible). */
    public static String maskGovernmentId(String id) {
        if (id == null || id.length() <= 5) {
            return "X".repeat(id == null ? 4 : id.length());
        }
        String prefix = id.substring(0, 3);
        String suffix = id.substring(id.length() - 2);
        return prefix + "X".repeat(id.length() - 5) + suffix;
    }
}
