package com.retailco.bankrag.service.dto;

import java.util.List;

/**
 * Response body for GET /api/v1/rag/search.
 *
 * guardrailTriggered / guardrailMessage implement the hallucination-risk
 * guardrail discussed in reports/hallucination-risk-analysis.md: when the
 * top result's score is below the configured similarity threshold, the
 * response carries no chunk content the caller/LLM should build an answer
 * from -- it explicitly signals "outside scope" instead of silently
 * returning weak or empty results (see hallucination-risk-analysis.md's
 * Recommendation 2, "score-margin / relative-confidence check": scoreMargin
 * is exposed here specifically so a calling layer can apply that check
 * rather than trusting the raw score alone).
 */
public record SearchResponse(
        String query,
        String method,
        List<SearchResultItem> results,
        boolean guardrailTriggered,
        String guardrailMessage,
        Double topScore,
        Double scoreMargin
) {
}
