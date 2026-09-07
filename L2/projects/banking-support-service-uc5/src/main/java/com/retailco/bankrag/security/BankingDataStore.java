package com.retailco.bankrag.security;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// CONCEPT: Repository pattern -- an in-memory stand-in for a real
// database-backed repository (what would normally be a Spring Data JPA
// `@Repository` talking to PostgreSQL).
// PURPOSE: Provides accounts, transactions, and loans keyed by customer/
// account id, using plain in-memory Maps seeded with demo data.
// WHY the method contracts matter: findAccountsByCustomerId,
// findTransactionsByAccountNumber, findLoansByCustomerId are written to
// look exactly like what a real Spring Data JPA repository interface
// would expose. That means a production version of this class could be
// replaced by a real `@Repository` backed by PostgreSQL without changing
// any of BankingToolService's calling code -- only this class's internals
// would change from "look up a Map" to "run a SQL query."
// IMPORTANT: this class holds no user input and no live external data --
// its only job is to simulate what a database would return, so the
// security/business logic above it (BankingToolService, PiiMasking) can
// be developed and tested against realistic-shaped data.
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
