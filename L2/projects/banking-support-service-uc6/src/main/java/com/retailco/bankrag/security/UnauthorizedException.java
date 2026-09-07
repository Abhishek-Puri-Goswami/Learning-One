package com.retailco.bankrag.security;

/**
 * Thrown for BOTH authentication failures (an invalid, expired, or
 * tampered token) AND authorization failures (a valid token for one
 * customer trying to access a different customer's data).
 * <p>
 * Using the SAME exception (and the same HTTP status) for both kinds of
 * failure is a deliberate security choice, not an oversight. If "your
 * token is invalid" and "you're not allowed to see this account"
 * returned different, distinguishable errors, an attacker could probe
 * many customer ids and use the DIFFERENCE in responses to figure out
 * which ids are real accounts. Giving both failures the exact same
 * response denies an attacker that signal.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
