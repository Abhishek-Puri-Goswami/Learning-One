package com.retailco.bankrag.security;

import com.retailco.bankrag.logging.StructuredAuditLogger;
import com.retailco.bankrag.logging.StructuredAuditLogger.AuditEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// CONCEPT: "Tool calling" -- exposing a fixed set of safe, deterministic
// operations (get_account_balance, get_transaction_history,
// get_loan_outstanding) instead of letting an LLM generate live data.
// PURPOSE: This is the ONE place live banking data leaves the system.
// Every public method: (1) authenticates + authorizes via
// verifyAndAuthorize() (now backed by AccessPolicy's RBAC decision, see
// below), (2) fetches raw data from BankingDataStore, (3) masks sensitive
// fields via PiiMasking before returning, (4) emits a structured audit
// record of the access decision.
//
// WHY SECURITY-CRITICAL: no method here ever routes through an LlmClient
// -- every value traces directly back to BankingDataStore. There is no
// code path by which a generative model could fabricate a balance.
//
// WHAT CHANGED FROM UC5 (see verifyAndAuthorize() below): UC5 checked
// "is this an ADMIN role?" inline. Here, that decision is delegated to
// AccessPolicy.evaluate(), a separately testable RBAC policy function that
// also understands SUPPORT_AGENT (read-only cross-customer access). This
// method's own job shrank to: verify the token itself (authentication),
// then call AccessPolicy for the authorization decision, then always emit
// an audit record either way (ALLOWED_SELF/ALLOWED_ELEVATED/
// DENIED_AUTHENTICATION/DENIED_AUTHORIZATION) -- see StructuredAuditLogger
// for where that record ends up.
//
// IMPORTANT (constructor overload for backward compatibility): the
// 2-argument constructor delegates to the 3-argument one with a null
// auditLogger, and audit() checks for null before logging. This lets
// older call sites that don't wire in an audit logger keep compiling and
// running with RBAC enforcement fully intact -- logging is treated as an
// optional add-on, never a security dependency.
public class BankingToolService {

    private final JwtService jwtService;
    private final BankingDataStore dataStore;
    private final StructuredAuditLogger auditLogger;

    public BankingToolService(JwtService jwtService, BankingDataStore dataStore) {
        this(jwtService, dataStore, null);
    }

    /**
     * L2 HLD UseCase6 constructor: wires in the structured audit trail
     * (docs/secure-backend-integration-design.md's "every access decision is
     * logged" requirement). {@code auditLogger} is nullable so every UC3/UC5
     * call site that predates this use case keeps compiling and running
     * unchanged -- RBAC enforcement below does not depend on logging being
     * configured.
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

        // L2 HLD UseCase6 RBAC: the authorization decision itself now lives
        // in AccessPolicy (independently unit-tested), not inline here --
        // this method's remaining job is auth (token validity) + emitting
        // the structured audit record, same split UC3 already had between
        // JwtService (authn) and this method (authz), just with authz's
        // logic extracted one level further.
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
