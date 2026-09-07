package com.retailco.bankrag.observability;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Deliverable: "Cost estimation." L2 HLD UseCase4 System Responsibilities:
 * "Estimate cost per query."
 *
 * Real arithmetic over real token counts (from EvaluationHarness/LlmClient
 * -- see L2/UC2's whitespace-tokenizer-approximation disclosure, which
 * applies here too, since these ARE the same token counts) against a
 * configurable per-1K-token rate. The rate itself is a realistic
 * *placeholder* for a mid-tier hosted LLM's blended input/output pricing
 * as of this submission's writing -- NOT a live-priced API call (there is
 * no reachable LLM billing API in this sandbox), and explicitly disclosed
 * as such in cost/cost-estimation-document.md rather than presented as
 * authoritative. A real deployment would source the actual rate from
 * whatever provider/contract is in force and update this constructor
 * argument accordingly -- the calculation itself doesn't change.
 */
public class CostEstimator {

    private final BigDecimal costPer1kPromptTokens;
    private final BigDecimal costPer1kCompletionTokens;

    public CostEstimator(BigDecimal costPer1kPromptTokens, BigDecimal costPer1kCompletionTokens) {
        this.costPer1kPromptTokens = costPer1kPromptTokens;
        this.costPer1kCompletionTokens = costPer1kCompletionTokens;
    }

    /** A reasonable, disclosed-as-illustrative default: $0.15 / 1K prompt tokens, $0.60 / 1K completion tokens (mid-tier hosted model blended rate order of magnitude). */
    public static CostEstimator illustrativeDefault() {
        return new CostEstimator(new BigDecimal("0.15"), new BigDecimal("0.60"));
    }

    public BigDecimal estimateCostUsd(int promptTokens, int completionTokens) {
        BigDecimal promptCost = costPer1kPromptTokens
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(BigDecimal.valueOf(1000), MathContext.DECIMAL64);
        BigDecimal completionCost = costPer1kCompletionTokens
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(BigDecimal.valueOf(1000), MathContext.DECIMAL64);
        return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
    }

    public BigDecimal projectMonthlyCost(BigDecimal avgCostPerAnsweredQuery, long answeredQueriesPerDay) {
        return avgCostPerAnsweredQuery.multiply(BigDecimal.valueOf(answeredQueriesPerDay))
                .multiply(BigDecimal.valueOf(30))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
