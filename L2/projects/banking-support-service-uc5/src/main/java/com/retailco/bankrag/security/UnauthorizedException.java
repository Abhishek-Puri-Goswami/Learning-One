package com.retailco.bankrag.security;

// CONCEPT: Custom unchecked exception, used here as a deliberate security
// design choice about information disclosure.
// PURPOSE: Thrown for BOTH authentication failures (invalid/expired/
// tampered JWT) AND authorization failures (a valid token for customer A
// trying to access customer B's data).
// WHY the SAME exception/HTTP status for two different failure kinds:
// this is a standard security practice, not an oversight. If "your token
// is invalid" and "you're not allowed to see this account" returned
// different errors, an attacker could probe many customer ids and use the
// different error responses to figure out which ids are real (an
// enumeration attack) -- collapsing both into one generic "unauthorized"
// response denies that signal.
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
