package com.retailco.bankrag.integration;

import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.observability.ObservableRagAssistant;
import com.retailco.bankrag.security.BankingToolService;
import com.retailco.bankrag.security.IntentClassifier;
import com.retailco.bankrag.security.UnauthorizedException;

import java.util.List;

/**
 * Deliverable: "Fully functional AI Banking Support System" / "End-to-end
 * workflow integration," per L2 HLD UseCase5. This is the one class in the
 * entire L2 submission that actually wires together every prior use case's
 * verified components into a single entry point:
 *
 *   L2/UC3's IntentClassifier   -- routes the query (unchanged from UC3)
 *   L2/UC4's ObservableRagAssistant -- policy questions (wraps L2/UC2's
 *                                      RagAssistant with UC4's caching/
 *                                      metrics/cost tracking)
 *   L2/UC3's BankingToolService -- live-data questions (JWT auth,
 *                                  authorization, masking -- unchanged)
 *
 * No new business logic is introduced here beyond routing and response
 * unification -- every guardrail, every retrieval algorithm, every
 * masking rule, every cache, every metric was already independently built
 * and verified in UC1-UC4. This class's only job is to prove those pieces
 * actually compose into one coherent system when called together, which
 * is precisely L2 HLD UseCase5's "System validation" functional scope
 * item.
 */
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
