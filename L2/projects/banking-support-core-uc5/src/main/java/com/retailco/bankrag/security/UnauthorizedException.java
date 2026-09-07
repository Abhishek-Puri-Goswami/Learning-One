package com.retailco.bankrag.security;

/**
 * Thrown for both authentication failures (invalid/expired/tampered token)
 * and authorization failures (a valid token for one customer requesting
 * another customer's data). Deliberately the SAME exception type/HTTP
 * status for both in the public API surface, per standard security
 * practice: returning a different error for "your token is invalid" vs.
 * "you're not allowed to see this other customer's account" would let an
 * attacker enumerate valid customer ids by observing which error comes
 * back (see security/security-validation-checklist.md item on this).
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
