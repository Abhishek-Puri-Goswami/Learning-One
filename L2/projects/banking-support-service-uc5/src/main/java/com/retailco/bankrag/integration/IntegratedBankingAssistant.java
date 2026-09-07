package com.retailco.bankrag.integration;

import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.observability.ObservableRagAssistant;
import com.retailco.bankrag.security.BankingToolService;
import com.retailco.bankrag.security.IntentClassifier;
import com.retailco.bankrag.security.UnauthorizedException;

import java.util.List;

// CONCEPT: Orchestrator / Facade -- the top-level entry point that ties
// intent classification, RAG, and secure banking tools into ONE unified
// API. This is the class to read FIRST to understand this whole module.
// PURPOSE: Given a raw query (plus auth context for live-data questions),
// route it to the right subsystem and return a single unified result type.
// FLOW (see handle() below): IntentClassifier decides the query type ->
// POLICY_QUESTION goes to ObservableRagAssistant (cached RAG over policy
// documents) -> ACCOUNT_BALANCE/TRANSACTION_HISTORY/LOAN_OUTSTANDING go to
// BankingToolService (JWT-checked, masked live data) -> AMBIGUOUS returns
// a clarifying message. No new business logic lives here -- every
// guardrail, retrieval algorithm, masking rule, cache, and metric was
// already built and verified in the pieces this class composes; this
// class's only job is routing and unifying their results.
//
// CONCEPT: sealed interface + exhaustive switch (a common modern-Java
// pattern for modeling "one of several possible outcomes"). `UnifiedResponse`
// can ONLY ever be one of PolicyAnswer/LiveDataAnswer/AccessDenied/
// Ambiguous -- the compiler enforces this, and the `switch` in handle()
// can be checked exhaustively against the Intent enum's cases without a
// `default` branch, so adding a new Intent value without updating this
// switch is a compile error, not a silent runtime bug.
//
// IMPORTANT (security note): UnauthorizedException from BankingToolService
// is caught HERE and converted into an AccessDenied result rather than
// propagating as an uncaught exception -- this keeps the unified API
// surface consistent (every path returns a UnifiedResponse) while still
// preserving the security decision (access was, in fact, denied).
public class IntegratedBankingAssistant {

    private final IntentClassifier intentClassifier;
    private final ObservableRagAssistant observableRagAssistant;
    private final BankingToolService bankingToolService;

    public IntegratedBankingAssistant(IntentClassifier intentClassifier, ObservableRagAssistant observableRagAssistant,
                                       BankingToolService bankingToolService) {
        this.intentClassifier = intentClassifier;
        this.observableRagAssistant = observableRagAssistant;
        this.bankingToolService = bankingToolService;
    }

    public sealed interface UnifiedResponse permits PolicyAnswer, LiveDataAnswer, AccessDenied, Ambiguous {
    }

    public record PolicyAnswer(RagAssistant.AssistantResponse ragResponse, boolean servedFromCache) implements UnifiedResponse {
    }

    public record LiveDataAnswer(IntentClassifier.Intent intent, Object data) implements UnifiedResponse {
    }

    public record AccessDenied(String reason) implements UnifiedResponse {
    }

    public record Ambiguous(String message) implements UnifiedResponse {
    }

    /**
     * @param query              the raw user question
     * @param bearerToken        JWT bearer token (only consulted for LIVE_DATA intents; a policy
     *                           question never touches authentication, matching L2/UC2's design
     *                           where RagAssistant has no auth dependency at all)
     * @param requestedCustomerId the customerId the caller is asking about (only relevant for LIVE_DATA intents)
     * @param accountNumber      only required for TRANSACTION_HISTORY; ignored otherwise
     */
    public UnifiedResponse handle(String query, String bearerToken, String requestedCustomerId, String accountNumber) {
        IntentClassifier.Intent intent = intentClassifier.classify(query);

        return switch (intent) {
            case POLICY_QUESTION -> {
                ObservableRagAssistant.ObservedResponse observed = observableRagAssistant.ask(query);
                yield new PolicyAnswer(observed.response(), observed.servedFromCache());
            }
            case ACCOUNT_BALANCE -> {
                try {
                    List<BankingToolService.MaskedAccount> accounts =
                            bankingToolService.getAccountBalance(bearerToken, requestedCustomerId);
                    yield new LiveDataAnswer(intent, accounts);
                } catch (UnauthorizedException e) {
                    yield new AccessDenied(e.getMessage());
                }
            }
            case TRANSACTION_HISTORY -> {
                try {
                    List<BankingToolService.MaskedTransaction> transactions =
                            bankingToolService.getTransactionHistory(bearerToken, requestedCustomerId, accountNumber, 10);
                    yield new LiveDataAnswer(intent, transactions);
                } catch (UnauthorizedException e) {
                    yield new AccessDenied(e.getMessage());
                }
            }
            case LOAN_OUTSTANDING -> {
                try {
                    List<BankingToolService.MaskedLoan> loans =
                            bankingToolService.getLoanOutstanding(bearerToken, requestedCustomerId);
                    yield new LiveDataAnswer(intent, loans);
                } catch (UnauthorizedException e) {
                    yield new AccessDenied(e.getMessage());
                }
            }
            case AMBIGUOUS -> new Ambiguous(
                    "This question seems to combine more than one type of request. "
                            + "Please ask about your account balance, transactions, loan outstanding, "
                            + "or a policy question separately.");
        };
    }
}
