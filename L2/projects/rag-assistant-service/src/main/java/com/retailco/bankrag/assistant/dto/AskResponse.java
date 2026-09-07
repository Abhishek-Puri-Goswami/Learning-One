package com.retailco.bankrag.assistant.dto;

import com.retailco.bankrag.assistant.CitationExtractor;
import com.retailco.bankrag.assistant.EvaluationHarness;

import java.util.List;

/**
 * Response body for POST /api/v1/assistant/ask. Mirrors
 * RagAssistant.AssistantResponse, exposed as a stable REST contract so
 * internal refactors of RagAssistant don't leak into the API shape.
 */
public record AskResponse(
        String query,
        String answer,
        List<CitationExtractor.Citation> citations,
        boolean blocked,
        String blockReason,
        boolean fallback,
        EvaluationHarness.EvaluationResult evaluation
) {
}
