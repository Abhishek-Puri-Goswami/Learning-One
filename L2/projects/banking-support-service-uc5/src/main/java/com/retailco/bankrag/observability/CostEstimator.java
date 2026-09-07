package com.retailco.bankrag.observability;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Turns real token counts into an estimated cost in US dollars, using a
 * configurable rate per 1,000 tokens (a separate rate for prompt vs.
 * completion tokens, since AI providers typically price them
 * differently).
 * <p>
 * Notice this class uses {@code BigDecimal} for the money math, not the
 * more common {@code double}. That's a deliberate choice: ordinary
 * floating-point math ({@code double}) can introduce tiny rounding errors
 * that add up over many calculations — unacceptable when the result
 * represents real money. {@code BigDecimal} gives exact, predictable
 * decimal arithmetic instead.
 * <p>
 * The rate used here is just a reasonable, illustrative placeholder, not
 * a live-priced quote from a real provider — a real deployment would plug
 * in its actual contracted rate, but the calculation logic itself would
 * stay exactly the same.
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
