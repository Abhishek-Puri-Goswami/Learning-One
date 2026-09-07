package com.retailco.bankrag.security;

import java.util.List;
import java.util.regex.Pattern;

// CONCEPT: Intent classification / request routing -- deciding WHICH
// subsystem should handle a query, before any expensive or risky work
// happens.
// PURPOSE: The single decision point between two completely different
// paths: a POLICY_QUESTION goes to RagAssistant (LLM-backed, retrieves
// from documents), while ACCOUNT_BALANCE/TRANSACTION_HISTORY/
// LOAN_OUTSTANDING go straight to BankingToolService (deterministic
// database lookups -- NEVER an LLM call).
//
// WHY THIS MATTERS FOR SECURITY: live financial data must never be
// generated/paraphrased by an LLM -- an LLM could hallucinate a wrong
// balance. Routing happens with plain regex pattern matching BEFORE any
// LLM is even considered, which makes it structurally impossible for a
// live-data question to accidentally reach a text-generation step.
//
// HOW IT WORKS (see classify() below): tests the query against three sets
// of regex patterns. If MORE THAN ONE category matches (e.g. "show my
// balance and my loan"), the result is AMBIGUOUS rather than guessing
// which one the user meant -- an ambiguous query should be split into
// separate requests, not silently answered with only one piece of it.
// Anything matching none of the live-data patterns defaults to
// POLICY_QUESTION, which has its own downstream guardrails.
//
// WHY pattern-based, not an LLM: same reasoning as PromptInjectionGuard/
// UnsafeQueryGuard in the assistant package -- deterministic, instant, and
// free, at the cost of only recognizing phrasings the patterns anticipate.
// A production system might add an LLM-based function-calling/tool-
// selection step for more flexible routing.
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
