package com.retailco.bankrag.security;

/**
 * The fixed set of roles this system understands, modeled as a Java
 * {@code enum} instead of plain text strings. Using an enum means the
 * compiler enforces a closed list — a typo like {@code "ADMN"} can't
 * silently compile and only fail later at runtime the way a raw string
 * comparison could — and every role that exists is visible in ONE place
 * (this file) instead of only being discoverable by searching every call
 * site.
 * <p>
 * A token may carry more than one of these roles at once (for example, a
 * special account issued both {@code SUPPORT_AGENT} and {@code ADMIN}) —
 * see {@link AccessPolicy}, which looks at the full set of roles a token
 * has, not just the first one it finds.
 */
public enum Role {
    /** The default role: can only access its own subject's account data. */
    CUSTOMER,
    /**
     * Read-only cross-customer access for support staff. Every value this
     * role can see was already masked before UC3's {@code BankingToolService}
     * methods return it -- SUPPORT_AGENT never grants visibility into raw
     * PII that CUSTOMER-scoped access wouldn't also have masked.
     */
    SUPPORT_AGENT,
    /** Full cross-customer access, unchanged from UC3's original "ADMIN" check. */
    ADMIN
}
