package com.retailco.bankrag.security;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Decides WHICH part of the system should handle a question, before any
 * expensive or risky work happens. A policy question goes to the AI
 * assistant (which retrieves and answers from documents), while a
 * question about account balance, transactions, or loans goes straight
 * to {@code BankingToolService} instead — a deterministic database
 * lookup that NEVER involves an AI model.
 * <p>
 * Why this matters for security: live financial data should never be
 * generated or paraphrased by an AI model, since it could get a number
 * wrong. By routing with simple pattern matching BEFORE any AI model is
 * even considered, it becomes structurally impossible for a live-data
 * question to accidentally end up being answered by text generation.
 * <p>
 * How {@code classify()} works: it checks the query against three sets
 * of patterns. If MORE THAN ONE category matches at once (like "show my
 * balance and my loan"), the result is AMBIGUOUS rather than guessing
 * which one the user actually wanted — a question mixing two intents
 * should be split into separate requests, not silently answered with
 * only one piece of it. Anything matching none of the live-data patterns
 * is treated as a policy question by default.
 */
public final class IntentClassifier {

    public enum Intent { POLICY_QUESTION, ACCOUNT_BALANCE, TRANSACTION_HISTORY, LOAN_OUTSTANDING, AMBIGUOUS }

    private static final List<Pattern> BALANCE_PATTERNS = List.of(
            Pattern.compile("account balance|my balance|how much (money |)do i have|current balance", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> TRANSACTION_PATTERNS = List.of(
            Pattern.compile("transaction history|recent transactions|my (recent |)transactions|statement of account|last \\d+ transactions", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> LOAN_PATTERNS = List.of(
            Pattern.compile("loan outstanding|outstanding (loan |)amount|how much (do i owe|is left) on my loan|remaining loan balance", Pattern.CASE_INSENSITIVE)
    );

    public Intent classify(String query) {
        boolean matchesBalance = matchesAny(query, BALANCE_PATTERNS);
        boolean matchesTransaction = matchesAny(query, TRANSACTION_PATTERNS);
        boolean matchesLoan = matchesAny(query, LOAN_PATTERNS);

        int matchCount = (matchesBalance ? 1 : 0) + (matchesTransaction ? 1 : 0) + (matchesLoan ? 1 : 0);
        if (matchCount > 1) {
            // A query matching more than one live-data category is treated
            // as ambiguous rather than guessed at -- e.g. "show my balance
            // and loan outstanding" would need to be split into two tool
            // calls by a real orchestrating agent, not silently answered
            // with only one of them.
            return Intent.AMBIGUOUS;
        }
        if (matchesBalance) return Intent.ACCOUNT_BALANCE;
        if (matchesTransaction) return Intent.TRANSACTION_HISTORY;
        if (matchesLoan) return Intent.LOAN_OUTSTANDING;

        // Default: anything not matching a live-data pattern is treated as
        // a policy question and routed to retrieval (L2/UC1 & UC2), which
        // has its own guardrails (weak-retrieval fallback, unsafe-advice
        // guard) for queries that turn out not to be answerable there either.
        return Intent.POLICY_QUESTION;
    }

    private boolean matchesAny(String query, List<Pattern> patterns) {
        return patterns.stream().anyMatch(p -> p.matcher(query).find());
    }
}
