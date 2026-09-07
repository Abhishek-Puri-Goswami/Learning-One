package com.retailco.bankrag.security;

import com.retailco.bankrag.logging.StructuredAuditLogger;
import com.retailco.bankrag.logging.StructuredAuditLogger.AuditEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * This is the ONE place live banking data ever leaves the system. Each
 * public method — getting an account balance, transaction history, or
 * loan info — does the same four things, in order: (1) authenticate and
 * authorize the caller (now backed by {@link AccessPolicy}'s RBAC
 * decision, see below), (2) fetch the raw data from
 * {@code BankingDataStore}, (3) mask the sensitive fields before
 * returning anything, (4) write a structured audit record of the access
 * decision.
 * <p>
 * Why this is security-critical: no method here ever routes through an
 * AI model — every value traces directly back to {@code BankingDataStore},
 * so there's no way a generative model could invent or misstate a
 * balance.
 * <p>
 * What's different from the earlier version of this project: previously,
 * the check was just "does this token have the ADMIN role?" inline. Now
 * that decision is handed off to {@code AccessPolicy.evaluate()}, a
 * separately testable policy function that also understands a
 * {@code SUPPORT_AGENT} role (read-only access across customers). This
 * method's own job shrank down to: verify the token itself
 * (authentication), ask {@code AccessPolicy} for the authorization
 * decision, then always write an audit record either way — see
 * {@code StructuredAuditLogger} for where that record ends up.
 * <p>
 * One detail worth noticing: there's a 2-argument constructor that
 * simply calls the 3-argument one with a {@code null} audit logger, and
 * {@code audit()} checks for {@code null} before logging anything. This
 * lets older code that doesn't wire in an audit logger keep working
 * exactly as before — logging is treated as an optional extra, never
 * something the security checks depend on.
 */
public class BankingToolService {

    private final JwtService jwtService;
    private final BankingDataStore dataStore;
    private final StructuredAuditLogger auditLogger;

    public BankingToolService(JwtService jwtService, BankingDataStore dataStore) {
        this(jwtService, dataStore, null);
    }

    /**
     * Constructor that also wires in the structured audit trail, so every
     * access decision gets logged. {@code auditLogger} can be {@code null}
     * so any older code that doesn't pass one keeps compiling and
     * running unchanged — the security checks below never depend on
     * logging being configured.
     */
    public BankingToolService(JwtService jwtService, BankingDataStore dataStore, StructuredAuditLogger auditLogger) {
        this.jwtService = jwtService;
        this.dataStore = dataStore;
        this.auditLogger = auditLogger;
    }

    public record MaskedAccount(String accountNumberMasked, String accountType, BigDecimal balance, String currency) {
    }

    public record MaskedTransaction(String transactionId, String date, String description, BigDecimal amount, String type) {
    }

    public record MaskedLoan(String loanId, String loanType, BigDecimal principal, BigDecimal outstanding, BigDecimal interestRate) {
    }

    /**
     * Tool: get_account_balance. Returns every account belonging to the
     * authenticated customer, with account numbers masked (last 4 digits
     * visible only).
     */
    public List<MaskedAccount> getAccountBalance(String bearerToken, String requestedCustomerId) {
        JwtService.Claims claims = verifyAndAuthorize(bearerToken, requestedCustomerId, "get_account_balance");
        return dataStore.findAccountsByCustomerId(requestedCustomerId).stream()
                .map(a -> new MaskedAccount(PiiMasking.maskAccountNumber(a.accountNumber()),
                        a.accountType(), a.balance(), a.currency()))
                .toList();
    }

    /**
     * Tool: get_transaction_history. Requires the caller to already know
     * (or have been given, via get_account_balance) the specific account
     * number -- this method still re-verifies that account belongs to the
     * authenticated customer before returning anything, rather than
     * trusting the caller's claim that it does (defense in depth against a
     * client passing an account number that isn't actually theirs).
     */
    public List<MaskedTransaction> getTransactionHistory(String bearerToken, String requestedCustomerId,
                                                           String accountNumber, int limit) {
        JwtService.Claims claims = verifyAndAuthorize(bearerToken, requestedCustomerId, "get_transaction_history");

        boolean accountBelongsToCustomer = dataStore.findAccountsByCustomerId(requestedCustomerId).stream()
                .anyMatch(a -> a.accountNumber().equals(accountNumber));
        if (!accountBelongsToCustomer) {
            throw new UnauthorizedException("Requested account does not belong to the authenticated customer");
        }

        return dataStore.findTransactionsByAccountNumber(accountNumber, limit).stream()
                .map(t -> new MaskedTransaction(t.transactionId(), t.date().toString(), t.description(), t.amount(), t.type()))
                .toList();
    }

    /** Tool: get_loan_outstanding. */
    public List<MaskedLoan> getLoanOutstanding(String bearerToken, String requestedCustomerId) {
        JwtService.Claims claims = verifyAndAuthorize(bearerToken, requestedCustomerId, "get_loan_outstanding");
        return dataStore.findLoansByCustomerId(requestedCustomerId).stream()
                .map(l -> new MaskedLoan(l.loanId(), l.loanType(), l.principal(), l.outstanding(), l.interestRate()))
                .toList();
    }

    /**
     * Shared auth + authorization gate for every tool method: the token
     * must be structurally valid, cryptographically unforged, unexpired
     * (authentication -- JwtService.verify), AND its subject claim must
     * match the customer id the caller is asking about, unless the token
     * carries an ADMIN role (authorization -- checked here, not delegated
     * to JwtService, since "which customer can this token act on behalf
     * of" is a business rule, not a token-format concern).
     */
    private JwtService.Claims verifyAndAuthorize(String bearerToken, String requestedCustomerId, String tool) {
        String correlationId = UUID.randomUUID().toString();
        JwtService.VerificationResult result = jwtService.verify(stripBearerPrefix(bearerToken));
        if (result instanceof JwtService.Invalid invalid) {
            audit(correlationId, "unknown", List.of(), requestedCustomerId, tool, "DENIED_AUTHENTICATION",
                    "Authentication failed: " + invalid.reason());
            throw new UnauthorizedException("Authentication failed: " + invalid.reason());
        }
        JwtService.Claims claims = ((JwtService.Valid) result).claims();

        // The actual authorization decision now lives in AccessPolicy
        // (its own independently-tested class) instead of being written
        // inline here. This method's job is simpler now: check the token
        // itself is valid, ask AccessPolicy who's allowed to see what,
        // and write an audit record either way.
        AccessPolicy.Decision decision = AccessPolicy.evaluate(claims.subject(), claims.roles(), requestedCustomerId);
        if (decision instanceof AccessPolicy.Denied denied) {
            audit(correlationId, claims.subject(), claims.roles(), requestedCustomerId, tool, "DENIED_AUTHORIZATION", denied.reason());
            throw new UnauthorizedException(denied.reason());
        }
        AccessPolicy.Allowed allowed = (AccessPolicy.Allowed) decision;
        audit(correlationId, claims.subject(), claims.roles(), requestedCustomerId, tool,
                allowed.elevated() ? "ALLOWED_ELEVATED" : "ALLOWED_SELF", allowed.reason());
        return claims;
    }

    private void audit(String correlationId, String actorSubject, List<String> actorRoles,
                        String requestedCustomerId, String tool, String decision, String reason) {
        if (auditLogger == null) return; // logging is an add-on (UC6); RBAC enforcement above never depends on it
        auditLogger.log(new AuditEvent(correlationId, "banking_tool_access", actorSubject, actorRoles,
                requestedCustomerId, tool, decision, reason));
    }

    private String stripBearerPrefix(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        return header;
    }
}
