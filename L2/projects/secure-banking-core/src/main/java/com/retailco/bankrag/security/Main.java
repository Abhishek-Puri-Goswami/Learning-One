package com.retailco.bankrag.security;

import java.util.List;

/**
 * CLI demo for L2 UC3 Secure Banking Data Integration. Exercises real
 * cryptographic JWT signing/verification, real authorization decisions,
 * real masking, and real routing -- every scenario below is an actual
 * program run, not a described one. Output captured verbatim in
 * reports/secure-banking-demo-run-log.txt.
 */
public class Main {

    public static void main(String[] args) {
        String secret = "demo-only-secret-key-not-for-production-use-32chars-min";
        JwtService jwtService = new JwtService(secret, 300); // 5-minute expiry
        BankingDataStore dataStore = new BankingDataStore();
        BankingToolService toolService = new BankingToolService(jwtService, dataStore);
        IntentClassifier intentClassifier = new IntentClassifier();

        System.out.println("=".repeat(100));
        System.out.println("SCENARIO 1: Intent classification routing");
        System.out.println("=".repeat(100));
        List<String> sampleQueries = List.of(
                "What is my account balance?",
                "Show me my recent transactions",
                "How much is left on my loan?",
                "What is the interest rate range for a home loan?",
                "Should I invest my savings in mutual funds right now?"
        );
        for (String q : sampleQueries) {
            System.out.println("QUERY: \"" + q + "\" -> " + intentClassifier.classify(q));
        }

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("SCENARIO 2: Legitimate token, own-customer data access (happy path)");
        System.out.println("=".repeat(100));
        String custToken = jwtService.issueToken("CUST1001", List.of("CUSTOMER"));
        System.out.println("Issued token for CUST1001: " + custToken);

        List<BankingToolService.MaskedAccount> accounts = toolService.getAccountBalance("Bearer " + custToken, "CUST1001");
        System.out.println("get_account_balance -> " + StructuredResponseFormatter.accountBalanceResponse("CUST1001", accounts));

        List<BankingToolService.MaskedTransaction> txns = toolService.getTransactionHistory(
                "Bearer " + custToken, "CUST1001", "100200300456", 3);
        System.out.println("get_transaction_history (limit 3) -> "
                + StructuredResponseFormatter.transactionHistoryResponse("CUST1001", PiiMasking.maskAccountNumber("100200300456"), txns));

        List<BankingToolService.MaskedLoan> loans = toolService.getLoanOutstanding("Bearer " + custToken, "CUST1001");
        System.out.println("get_loan_outstanding -> " + StructuredResponseFormatter.loanOutstandingResponse("CUST1001", loans));

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("SCENARIO 3: Cross-customer access attempt (authorization failure, real rejection)");
        System.out.println("=".repeat(100));
        try {
            toolService.getAccountBalance("Bearer " + custToken, "CUST1002"); // CUST1001's token, asking for CUST1002's data
            System.out.println("UNEXPECTED: request should have been rejected");
        } catch (UnauthorizedException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("SCENARIO 4: Tampered/forged token (real cryptographic verification failure)");
        System.out.println("=".repeat(100));
        JwtService attackerJwtService = new JwtService("a-completely-different-attacker-controlled-secret-key", 300);
        String forgedToken = attackerJwtService.issueToken("CUST1001", List.of("ADMIN")); // attacker tries to grant themselves ADMIN
        try {
            toolService.getAccountBalance("Bearer " + forgedToken, "CUST1001");
            System.out.println("UNEXPECTED: forged token should have been rejected");
        } catch (UnauthorizedException e) {
            System.out.println("Correctly rejected forged token: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("SCENARIO 5: Expired token (real clock-based expiry check)");
        System.out.println("=".repeat(100));
        JwtService shortLivedJwtService = new JwtService(secret, -1); // expiresAt in the past immediately
        String expiredToken = shortLivedJwtService.issueToken("CUST1001", List.of("CUSTOMER"));
        try {
            toolService.getAccountBalance("Bearer " + expiredToken, "CUST1001");
            System.out.println("UNEXPECTED: expired token should have been rejected");
        } catch (UnauthorizedException e) {
            System.out.println("Correctly rejected expired token: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("SCENARIO 6: ADMIN role can access any customer's data");
        System.out.println("=".repeat(100));
        String adminToken = jwtService.issueToken("SUPPORT_AGENT_7", List.of("ADMIN"));
        List<BankingToolService.MaskedAccount> adminView = toolService.getAccountBalance("Bearer " + adminToken, "CUST1002");
        System.out.println("ADMIN get_account_balance(CUST1002) -> "
                + StructuredResponseFormatter.accountBalanceResponse("CUST1002", adminView));

        System.out.println();
        System.out.println("All scenarios completed.");
    }
}
