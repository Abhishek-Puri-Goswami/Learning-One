package com.retailco.bankrag.rbac;

import com.retailco.bankrag.logging.StructuredAuditLogger;
import com.retailco.bankrag.security.AccessPolicy;
import com.retailco.bankrag.security.BankingDataStore;
import com.retailco.bankrag.security.BankingToolService;
import com.retailco.bankrag.security.JwtService;
import com.retailco.bankrag.security.Role;
import com.retailco.bankrag.security.UnauthorizedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Deliverable: "Masking security tests" + "Unauthorized access tests" (L2
 * HLD UseCase6 section 8.4), and the RBAC half of "Validate JWT logic"
 * (section 6.1's CI/CD pipeline requirement -- this is exactly the suite
 * that pipeline step is meant to run). Deliberately independent of
 * banking-support-core's existing {@code integration.SelfTests} (which
 * needs the real corpus on disk for RAG routing): every test here only
 * needs {@link AccessPolicy}, {@link BankingToolService}, and
 * {@link StructuredAuditLogger}, so it can run as its own fast CI stage.
 */
public class RbacAndAuditSelfTests {

    private static int passed = 0;
    private static int failed = 0;
    private static final String JWT_SECRET = "rbac-test-secret-key-at-least-32-characters-long";

    public static void main(String[] args) throws IOException {
        testCustomerSelfAccessIsAllowed();
        testCustomerCannotAccessOtherCustomer();
        testSupportAgentGetsElevatedReadOnlyAccessToOtherCustomer();
        testAdminGetsElevatedAccessToOtherCustomer();
        testUnknownRoleCannotAccessOtherCustomer();
        testUnknownRoleCanStillAccessOwnData();
        testMultiRoleTokenGrantsAccessIfAnyRoleQualifies();

        testSupportAgentCanReadOtherCustomersBalanceViaToolService();
        testSupportAgentReceivedDataIsStillMasked();
        testPlainCustomerToolServiceCallDeniedForOtherCustomer();

        testAuditLoggerWritesOneJsonObjectPerLine();
        testAuditLogNeverContainsRawAccountNumber();
        testAuditLogRecordsDeniedAuthenticationAttempt();
        testAuditLogRecordsElevatedAccessDecision();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------- AccessPolicy (pure function, no I/O) ----------

    private static void testCustomerSelfAccessIsAllowed() {
        AccessPolicy.Decision d = AccessPolicy.evaluate("CUST1001", List.of("CUSTOMER"), "CUST1001");
        check("CUSTOMER accessing own data is allowed", d instanceof AccessPolicy.Allowed);
    }

    private static void testCustomerCannotAccessOtherCustomer() {
        AccessPolicy.Decision d = AccessPolicy.evaluate("CUST1001", List.of("CUSTOMER"), "CUST1002");
        check("CUSTOMER accessing another customer's data is denied", d instanceof AccessPolicy.Denied);
    }

    private static void testSupportAgentGetsElevatedReadOnlyAccessToOtherCustomer() {
        AccessPolicy.Decision d = AccessPolicy.evaluate("AGENT-042", List.of("SUPPORT_AGENT"), "CUST1002");
        check("SUPPORT_AGENT accessing another customer's data is allowed",
                d instanceof AccessPolicy.Allowed a && a.elevated());
    }

    private static void testAdminGetsElevatedAccessToOtherCustomer() {
        AccessPolicy.Decision d = AccessPolicy.evaluate("ADMIN-01", List.of("ADMIN"), "CUST1002");
        check("ADMIN accessing another customer's data is allowed",
                d instanceof AccessPolicy.Allowed a && a.elevated());
    }

    private static void testUnknownRoleCannotAccessOtherCustomer() {
        AccessPolicy.Decision d = AccessPolicy.evaluate("SVC-01", List.of("BILLING_BOT"), "CUST1002");
        check("An unrecognized role grants no cross-customer access", d instanceof AccessPolicy.Denied);
    }

    private static void testUnknownRoleCanStillAccessOwnData() {
        AccessPolicy.Decision d = AccessPolicy.evaluate("CUST1001", List.of("BILLING_BOT"), "CUST1001");
        check("Self-access is allowed even with an unrecognized role", d instanceof AccessPolicy.Allowed);
    }

    private static void testMultiRoleTokenGrantsAccessIfAnyRoleQualifies() {
        AccessPolicy.Decision d = AccessPolicy.evaluate("SVC-02", List.of("BILLING_BOT", "SUPPORT_AGENT"), "CUST1002");
        check("A token with a mix of unknown and qualifying roles is allowed",
                d instanceof AccessPolicy.Allowed a && a.grantingRole() == Role.SUPPORT_AGENT);
    }

    // ---------- BankingToolService (real JWTs + real in-memory data) ----------

    private static void testSupportAgentCanReadOtherCustomersBalanceViaToolService() {
        BankingToolService toolService = new BankingToolService(new JwtService(JWT_SECRET, 900), new BankingDataStore());
        String token = issueToken("AGENT-042", "SUPPORT_AGENT");
        var result = toolService.getAccountBalance("Bearer " + token, "CUST1001");
        check("SUPPORT_AGENT token can retrieve CUST1001's balance", !result.isEmpty());
    }

    private static void testSupportAgentReceivedDataIsStillMasked() {
        BankingToolService toolService = new BankingToolService(new JwtService(JWT_SECRET, 900), new BankingDataStore());
        String token = issueToken("AGENT-042", "SUPPORT_AGENT");
        var result = toolService.getAccountBalance("Bearer " + token, "CUST1001");
        boolean allMasked = result.stream().allMatch(a -> a.accountNumberMasked().contains("X"));
        check("SUPPORT_AGENT's elevated access still only sees masked account numbers", allMasked);
    }

    private static void testPlainCustomerToolServiceCallDeniedForOtherCustomer() {
        BankingToolService toolService = new BankingToolService(new JwtService(JWT_SECRET, 900), new BankingDataStore());
        String token = issueToken("CUST1001", "CUSTOMER");
        boolean threw = false;
        try {
            toolService.getAccountBalance("Bearer " + token, "CUST1002");
        } catch (UnauthorizedException e) {
            threw = true;
        }
        check("CUSTOMER token requesting another customer's balance throws UnauthorizedException", threw);
    }

    // ---------- StructuredAuditLogger ----------

    private static void testAuditLoggerWritesOneJsonObjectPerLine() throws IOException {
        Path tmp = Files.createTempFile("audit-test", ".jsonl");
        StructuredAuditLogger logger = new StructuredAuditLogger(tmp);
        logger.log(new StructuredAuditLogger.AuditEvent("c1", "banking_tool_access", "CUST1001",
                List.of("CUSTOMER"), "CUST1001", "get_account_balance", "ALLOWED_SELF", "self access"));
        logger.log(new StructuredAuditLogger.AuditEvent("c2", "banking_tool_access", "AGENT-042",
                List.of("SUPPORT_AGENT"), "CUST1002", "get_account_balance", "ALLOWED_ELEVATED", "support agent access"));
        List<String> lines = Files.readAllLines(tmp, StandardCharsets.UTF_8);
        boolean twoLines = lines.size() == 2;
        boolean bothParse = lines.stream().allMatch(l -> l.startsWith("{") && l.endsWith("}"));
        check("StructuredAuditLogger writes exactly one JSON object per log() call", twoLines && bothParse);
    }

    private static void testAuditLogNeverContainsRawAccountNumber() throws IOException {
        Path tmp = Files.createTempFile("audit-test-masking", ".jsonl");
        StructuredAuditLogger logger = new StructuredAuditLogger(tmp);
        BankingToolService toolService = new BankingToolService(new JwtService(JWT_SECRET, 900), new BankingDataStore(), logger);
        String token = issueToken("CUST1001", "CUSTOMER");
        var accounts = toolService.getAccountBalance("Bearer " + token, "CUST1001");
        String rawAccountNumber = new BankingDataStore().findAccountsByCustomerId("CUST1001").get(0).accountNumber();

        String logContent = Files.readString(tmp, StandardCharsets.UTF_8);
        boolean noRawAccountNumberLogged = !logContent.contains(rawAccountNumber);
        check("Audit log never contains a raw (unmasked) account number -- only decision metadata", noRawAccountNumberLogged);
    }

    private static void testAuditLogRecordsDeniedAuthenticationAttempt() throws IOException {
        Path tmp = Files.createTempFile("audit-test-authn", ".jsonl");
        StructuredAuditLogger logger = new StructuredAuditLogger(tmp);
        BankingToolService toolService = new BankingToolService(new JwtService(JWT_SECRET, 900), new BankingDataStore(), logger);
        try {
            toolService.getAccountBalance("Bearer not-a-real-token", "CUST1001");
        } catch (UnauthorizedException ignored) {
        }
        String logContent = Files.readString(tmp, StandardCharsets.UTF_8);
        check("A forged/malformed token produces a DENIED_AUTHENTICATION audit record",
                logContent.contains("DENIED_AUTHENTICATION"));
    }

    private static void testAuditLogRecordsElevatedAccessDecision() throws IOException {
        Path tmp = Files.createTempFile("audit-test-elevated", ".jsonl");
        StructuredAuditLogger logger = new StructuredAuditLogger(tmp);
        BankingToolService toolService = new BankingToolService(new JwtService(JWT_SECRET, 900), new BankingDataStore(), logger);
        String token = issueToken("AGENT-042", "SUPPORT_AGENT");
        toolService.getAccountBalance("Bearer " + token, "CUST1001");
        String logContent = Files.readString(tmp, StandardCharsets.UTF_8);
        check("A SUPPORT_AGENT accessing another customer's data produces an ALLOWED_ELEVATED audit record",
                logContent.contains("ALLOWED_ELEVATED") && logContent.contains("AGENT-042"));
    }

    // ---------- helpers ----------

    private static String issueToken(String subject, String role) {
        return new JwtService(JWT_SECRET, 900).issueToken(subject, List.of(role));
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
