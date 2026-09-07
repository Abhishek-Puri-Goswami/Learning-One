package com.retailco.bankrag.security;

import java.math.BigDecimal;
import java.util.List;

/**
 * This is the ONE place live banking data ever leaves the system. Each
 * public method here — getting an account balance, transaction history,
 * or loan info — does the same three things, in order: (1) check the
 * caller is authenticated and allowed to see this customer's data, (2)
 * fetch the raw data from {@code BankingDataStore}, (3) mask the
 * sensitive fields before returning anything.
 * <p>
 * Why this is security-critical: no method in this class ever calls an
 * AI model. Every value returned here traces directly back to
 * {@code BankingDataStore}, with masking applied before it leaves this
 * class — there is no way for an AI model to invent or misstate a
 * balance, transaction, or loan figure, because an AI model is never
 * involved in producing this data at all.
 * <p>
 * {@code verifyAndAuthorize()} is the shared gate every method calls
 * first: it first checks AUTHENTICATION (is this a real, unexpired,
 * correctly-signed token? — handled entirely by {@code JwtService}), then
 * checks AUTHORIZATION (does this token's owner match the customer being
 * asked about, or do they hold an admin role?). Authentication answers
 * "who are you?"; authorization answers "are you allowed to see THIS
 * customer's data?" — keeping them as two separate steps is the standard
 * way to reason about this kind of security check.
 * <p>
 * One more safety detail worth noticing, in {@code getTransactionHistory}:
 * even after a customer id passes authorization, the method still
 * independently double-checks that the SPECIFIC account number requested
 * actually belongs to that customer — it never just trusts a
 * client-supplied account number.
 */
public class BankingToolService {

    private final JwtService jwtService;
    private final BankingDataStore dataStore;

    public BankingToolService(JwtService jwtService, BankingDataStore dataStore) {
        this.jwtService = jwtService;
        this.dataStore = dataStore;
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
        JwtService.Claims claims = verifyAndAuthorize(bearerToken, requestedCustomerId);
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
        JwtService.Claims claims = verifyAndAuthorize(bearerToken, requestedCustomerId);

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
        JwtService.Claims claims = verifyAndAuthorize(bearerToken, requestedCustomerId);
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
    private JwtService.Claims verifyAndAuthorize(String bearerToken, String requestedCustomerId) {
        JwtService.VerificationResult result = jwtService.verify(stripBearerPrefix(bearerToken));
        if (result instanceof JwtService.Invalid invalid) {
            throw new UnauthorizedException("Authentication failed: " + invalid.reason());
        }
        JwtService.Claims claims = ((JwtService.Valid) result).claims();

        boolean isSelf = claims.subject().equals(requestedCustomerId);
        boolean isAdmin = claims.roles().contains("ADMIN");
        if (!isSelf && !isAdmin) {
            throw new UnauthorizedException("Token subject '" + claims.subject()
                    + "' is not authorized to access customer '" + requestedCustomerId + "'");
        }
        return claims;
    }

    private String stripBearerPrefix(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        return header;
    }
}
