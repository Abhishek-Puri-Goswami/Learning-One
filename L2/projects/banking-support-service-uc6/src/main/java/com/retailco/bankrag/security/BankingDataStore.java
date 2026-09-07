package com.retailco.bankrag.security;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory stand-in for the "relational database" L2 HLD UseCase3's
 * Implementation Approach names ("Fetch data from a relational database").
 * A real deployment would back this with the same PostgreSQL instance
 * L1/L2 already standardize on (see L2/UC1's design/vector-database-schema.md
 * for that same reuse-existing-Postgres reasoning) via Spring Data JPA
 * repositories -- not reachable/buildable here (Maven Central blocked), so
 * this class keeps the exact same method contracts a
 * `@Repository`-backed implementation would have, letting
 * secure-banking-service's real Spring wiring swap this out without
 * changing any calling code.
 */
public class BankingDataStore {

    public record Account(String accountNumber, String customerId, String accountType, BigDecimal balance, String currency) {
    }

    public record Transaction(String transactionId, String accountNumber, LocalDate date, String description, BigDecimal amount, String type) {
    }

    public record Loan(String loanId, String customerId, String loanType, BigDecimal principal, BigDecimal outstanding, BigDecimal interestRate) {
    }

    private final Map<String, List<Account>> accountsByCustomer = new LinkedHashMap<>();
    private final Map<String, List<Transaction>> transactionsByAccount = new LinkedHashMap<>();
    private final Map<String, List<Loan>> loansByCustomer = new LinkedHashMap<>();

    public BankingDataStore() {
        seedDemoData();
    }

    public List<Account> findAccountsByCustomerId(String customerId) {
        return accountsByCustomer.getOrDefault(customerId, List.of());
    }

    public List<Transaction> findTransactionsByAccountNumber(String accountNumber, int limit) {
        List<Transaction> all = transactionsByAccount.getOrDefault(accountNumber, List.of());
        return all.size() > limit ? all.subList(0, limit) : all;
    }

    public List<Loan> findLoansByCustomerId(String customerId) {
        return loansByCustomer.getOrDefault(customerId, List.of());
    }

    private void seedDemoData() {
        accountsByCustomer.put("CUST1001", List.of(
                new Account("100200300456", "CUST1001", "SAVINGS", new BigDecimal("184250.75"), "INR"),
                new Account("100200300789", "CUST1001", "CURRENT", new BigDecimal("52310.00"), "INR")
        ));
        transactionsByAccount.put("100200300456", List.of(
                new Transaction("TXN9001", "100200300456", LocalDate.of(2026, 8, 25), "Salary Credit", new BigDecimal("95000.00"), "CREDIT"),
                new Transaction("TXN9002", "100200300456", LocalDate.of(2026, 8, 24), "Electricity Bill Payment", new BigDecimal("-3200.50"), "DEBIT"),
                new Transaction("TXN9003", "100200300456", LocalDate.of(2026, 8, 20), "ATM Withdrawal", new BigDecimal("-10000.00"), "DEBIT"),
                new Transaction("TXN9004", "100200300456", LocalDate.of(2026, 8, 15), "Interest Credit", new BigDecimal("412.25"), "CREDIT")
        ));
        loansByCustomer.put("CUST1001", List.of(
                new Loan("LOAN5001", "CUST1001", "HOME_LOAN", new BigDecimal("4500000.00"), new BigDecimal("3120450.00"), new BigDecimal("8.75"))
        ));

        accountsByCustomer.put("CUST1002", List.of(
                new Account("100200300999", "CUST1002", "SAVINGS", new BigDecimal("12800.00"), "INR")
        ));
        transactionsByAccount.put("100200300999", List.of(
                new Transaction("TXN9101", "100200300999", LocalDate.of(2026, 8, 26), "Grocery Store Purchase", new BigDecimal("-1450.00"), "DEBIT")
        ));
    }
}
