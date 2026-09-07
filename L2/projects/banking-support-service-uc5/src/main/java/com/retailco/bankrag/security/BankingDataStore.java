package com.retailco.bankrag.security;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory stand-in for what would normally be a real database-backed
 * repository. It provides accounts, transactions, and loans keyed by
 * customer or account id, using simple in-memory maps seeded with demo
 * data.
 * <p>
 * Its methods ({@code findAccountsByCustomerId},
 * {@code findTransactionsByAccountNumber},
 * {@code findLoansByCustomerId}) are written to look exactly like what a
 * real database repository would expose. That means a production version
 * of this class could later be swapped in — one backed by a real database
 * — without any of {@code BankingToolService}'s calling code needing to
 * change at all; only this class's own internals would change from
 * "look up a map" to "run a query."
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
