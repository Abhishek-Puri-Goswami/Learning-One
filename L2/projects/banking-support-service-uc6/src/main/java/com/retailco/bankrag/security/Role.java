package com.retailco.bankrag.security;

// CONCEPT: RBAC (Role-Based Access Control) -- modeling roles as a Java
// `enum` rather than free-floating strings.
// PURPOSE: Defines the closed, fixed set of roles the system recognizes.
// WHY an enum instead of a String constant like "ADMIN": an enum is a
// closed set the compiler enforces -- a typo like "ADMN" can't silently
// compile and fail at runtime the way a raw string comparison could, and
// every role that exists is visible in ONE place (this file) instead of
// being discoverable only by grepping every call site.
/**
 * Deliverable: "RBAC (Role-Based Access Control)." L2 HLD UseCase6
 * Functional Scope: "Secure backend API access (JWT, RBAC)."
 *
 * UC3 only ever checked for a single role string, "ADMIN", inline inside
 * {@code BankingToolService.verifyAndAuthorize}. UC6 promotes that into an
 * explicit, closed set of roles with a documented permission model (see
 * {@link AccessPolicy} and docs/secure-backend-integration-design.md's RBAC
 * matrix) -- a named enum rather than a free-floating string is what makes
 * "what can a SUPPORT_AGENT do" a reviewable, testable question instead of
 * something only discoverable by reading every call site.
 *
 * A token may carry more than one of these (e.g. a break-glass account
 * issued both SUPPORT_AGENT and ADMIN) -- {@link AccessPolicy} evaluates
 * the full role set, not just the first match.
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
