package com.retailco.bankrag.observability;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

// CONCEPT: Cost estimation using BigDecimal (not double/float) for money math.
// PURPOSE: Converts real token counts into an estimated USD cost, using a
// configurable per-1000-token rate (separate rates for prompt vs.
// completion tokens, since providers typically price them differently).
// WHY BigDecimal instead of double: floating-point (double) arithmetic can
// introduce small rounding errors that compound over many calculations --
// unacceptable when the result represents money. BigDecimal with an
// explicit RoundingMode/scale gives exact, predictable decimal arithmetic.
// IMPORTANT: the per-1K-token rate is a configurable, disclosed
// *placeholder* for illustration, not a live-priced API quote -- a real
// deployment would source the actual rate from its provider/contract, but
// the calculation logic itself (estimateCostUsd/projectMonthlyCost) would
// stay the same.
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
