package com.retailco.bankrag.security;

import java.util.List;

/**
 * Hand-rolled test harness (JUnit unreachable via Maven Central in this
 * sandbox, same limitation disclosed throughout this submission). Prints
 * [PASS]/[FAIL] per check, exits non-zero on any failure.
 */
public class SelfTests {

    private static int passed = 0;
    private static int failed = 0;

    private static final String SECRET = "test-secret-key-at-least-32-characters-long-1234";

    public static void main(String[] args) {
        testJwtRoundTripValid();
        testJwtRejectsTamperedSignature();
        testJwtRejectsWrongSecret();
        testJwtRejectsExpiredToken();
        testJwtRejectsMalformedToken();
        testJwtConstructorRejectsShortSecret();
        testPiiMaskingAccountNumber();
        testPiiMaskingEmail();
        testPiiMaskingGovernmentId();
        testIntentClassifierRoutesBalanceQuery();
        testIntentClassifierRoutesTransactionQuery();
        testIntentClassifierRoutesLoanQuery();
        testIntentClassifierRoutesPolicyQuestionByDefault();
        testIntentClassifierFlagsAmbiguousMultiIntentQuery();
        testToolServiceAllowsOwnDataAccess();
        testToolServiceBlocksCrossCustomerAccess();
        testToolServiceAllowsAdminCrossCustomerAccess();
        testToolServiceBlocksAccountNotBelongingToCustomer();
        testToolServiceMasksAccountNumberInResponse();
        testStructuredResponseFormatterProducesValidJson();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testJwtRoundTripValid() {
        JwtService jwt = new JwtService(SECRET, 300);
        String token = jwt.issueToken("CUST1001", List.of("CUSTOMER"));
        JwtService.VerificationResult result = jwt.verify(token);
        check("jwt: valid token verifies", result instanceof JwtService.Valid);
        if (result instanceof JwtService.Valid v) {
            check("jwt: subject round-trips", v.claims().subject().equals("CUST1001"));
            check("jwt: roles round-trip", v.claims().roles().contains("CUSTOMER"));
        }
    }

    private static void testJwtRejectsTamperedSignature() {
        JwtService jwt = new JwtService(SECRET, 300);
        String token = jwt.issueToken("CUST1001", List.of("CUSTOMER"));
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + "ZmFrZXNpZ25hdHVyZQ"; // garbage signature
        JwtService.VerificationResult result = jwt.verify(tampered);
        check("jwt: tampered signature rejected", result instanceof JwtService.Invalid);
    }

    private static void testJwtRejectsWrongSecret() {
        JwtService issuer = new JwtService(SECRET, 300);
        JwtService verifier = new JwtService("a-totally-different-secret-key-of-32-chars-min", 300);
        String token = issuer.issueToken("CUST1001", List.of("CUSTOMER"));
        check("jwt: token signed with different secret rejected", verifier.verify(token) instanceof JwtService.Invalid);
    }

    private static void testJwtRejectsExpiredToken() {
        JwtService jwt = new JwtService(SECRET, -5); // already expired at issuance
        String token = jwt.issueToken("CUST1001", List.of("CUSTOMER"));
        JwtService.VerificationResult result = jwt.verify(token);
        check("jwt: expired token rejected", result instanceof JwtService.Invalid);
    }

    private static void testJwtRejectsMalformedToken() {
        JwtService jwt = new JwtService(SECRET, 300);
        check("jwt: malformed token (no dots) rejected", jwt.verify("not-a-jwt-at-all") instanceof JwtService.Invalid);
        check("jwt: null token rejected", jwt.verify(null) instanceof JwtService.Invalid);
    }

    private static void testJwtConstructorRejectsShortSecret() {
        boolean threw = false;
        try {
            new JwtService("too-short", 300);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("jwt: constructor rejects secret shorter than 32 chars", threw);
    }

    private static void testPiiMaskingAccountNumber() {
        check("masking: account number keeps last 4 only",
                PiiMasking.maskAccountNumber("100200300456").equals("XXXXXXXX0456"));
    }

    private static void testPiiMaskingEmail() {
        String masked = PiiMasking.maskEmail("anaya.sharma@example.com");
        check("masking: email keeps domain, masks most of local part",
                masked.equals("an***@example.com"));
    }

    private static void testPiiMaskingGovernmentId() {
        String masked = PiiMasking.maskGovernmentId("ABCPD1234E");
        check("masking: government id keeps first 3 and last 2",
                masked.equals("ABCXXXXX4E"));
    }

    private static void testIntentClassifierRoutesBalanceQuery() {
        check("intent: balance query routed correctly",
                new IntentClassifier().classify("What is my account balance?") == IntentClassifier.Intent.ACCOUNT_BALANCE);
    }

    private static void testIntentClassifierRoutesTransactionQuery() {
        check("intent: transaction query routed correctly",
                new IntentClassifier().classify("Show me my recent transactions") == IntentClassifier.Intent.TRANSACTION_HISTORY);
    }

    private static void testIntentClassifierRoutesLoanQuery() {
        check("intent: loan outstanding query routed correctly",
                new IntentClassifier().classify("How much is left on my loan?") == IntentClassifier.Intent.LOAN_OUTSTANDING);
    }

    private static void testIntentClassifierRoutesPolicyQuestionByDefault() {
        check("intent: policy question routed correctly",
                new IntentClassifier().classify("What is the interest rate range for a home loan?") == IntentClassifier.Intent.POLICY_QUESTION);
    }

    private static void testIntentClassifierFlagsAmbiguousMultiIntentQuery() {
        check("intent: multi-intent query flagged ambiguous",
                new IntentClassifier().classify("Show me my account balance and my loan outstanding amount")
                        == IntentClassifier.Intent.AMBIGUOUS);
    }

    private static void testToolServiceAllowsOwnDataAccess() {
        BankingToolService service = buildService();
        JwtService jwt = new JwtService(SECRET, 300);
        String token = jwt.issueToken("CUST1001", List.of("CUSTOMER"));
        List<BankingToolService.MaskedAccount> accounts = service.getAccountBalance("Bearer " + token, "CUST1001");
        check("tool: own-customer access returns accounts", !accounts.isEmpty());
    }

    private static void testToolServiceBlocksCrossCustomerAccess() {
        BankingToolService service = buildService();
        JwtService jwt = new JwtService(SECRET, 300);
        String token = jwt.issueToken("CUST1001", List.of("CUSTOMER"));
        boolean threw = false;
        try {
            service.getAccountBalance("Bearer " + token, "CUST1002");
        } catch (UnauthorizedException e) {
            threw = true;
        }
        check("tool: cross-customer access blocked", threw);
    }

    private static void testToolServiceAllowsAdminCrossCustomerAccess() {
        BankingToolService service = buildService();
        JwtService jwt = new JwtService(SECRET, 300);
        String adminToken = jwt.issueToken("SUPPORT_AGENT_7", List.of("ADMIN"));
        List<BankingToolService.MaskedAccount> accounts = service.getAccountBalance("Bearer " + adminToken, "CUST1002");
        check("tool: ADMIN role allowed cross-customer access", !accounts.isEmpty());
    }

    private static void testToolServiceBlocksAccountNotBelongingToCustomer() {
        BankingToolService service = buildService();
        JwtService jwt = new JwtService(SECRET, 300);
        String token = jwt.issueToken("CUST1001", List.of("CUSTOMER"));
        boolean threw = false;
        try {
            // CUST1001's token, but requesting CUST1002's account number
            service.getTransactionHistory("Bearer " + token, "CUST1001", "100200300999", 5);
        } catch (UnauthorizedException e) {
            threw = true;
        }
        check("tool: account-ownership mismatch blocked even for correct customer token", threw);
    }

    private static void testToolServiceMasksAccountNumberInResponse() {
        BankingToolService service = buildService();
        JwtService jwt = new JwtService(SECRET, 300);
        String token = jwt.issueToken("CUST1001", List.of("CUSTOMER"));
        List<BankingToolService.MaskedAccount> accounts = service.getAccountBalance("Bearer " + token, "CUST1001");
        boolean allMasked = accounts.stream().allMatch(a -> a.accountNumberMasked().startsWith("X"));
        check("tool: returned account numbers are masked, not raw", allMasked);
    }

    private static void testStructuredResponseFormatterProducesValidJson() {
        BankingToolService service = buildService();
        JwtService jwt = new JwtService(SECRET, 300);
        String token = jwt.issueToken("CUST1001", List.of("CUSTOMER"));
        List<BankingToolService.MaskedAccount> accounts = service.getAccountBalance("Bearer " + token, "CUST1001");
        String json = StructuredResponseFormatter.accountBalanceResponse("CUST1001", accounts);
        check("formatter: output starts and ends as a JSON object", json.startsWith("{") && json.endsWith("}"));
        check("formatter: output contains customerId field", json.contains("\"customerId\":\"CUST1001\""));
    }

    private static BankingToolService buildService() {
        return new BankingToolService(new JwtService(SECRET, 300), new BankingDataStore());
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name);
            failed++;
        }
    }
}
