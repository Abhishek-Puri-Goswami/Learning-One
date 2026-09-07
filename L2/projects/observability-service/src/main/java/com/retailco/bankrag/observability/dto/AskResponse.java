package com.retailco.bankrag.observability.dto;

import com.retailco.bankrag.assistant.CitationExtractor;
import com.retailco.bankrag.assistant.EvaluationHarness;

import java.util.List;

/**
 * The response for an answered question — the same shape as
 * {@code rag-assistant-service}'s {@code AskResponse}, plus one extra
 * field, {@code servedFromCache}, so API callers can see whether this
 * particular answer came straight from the cache.
 */
public record AskResponse(
        String query,
        String answer,
        List<CitationExtractor.Citation> citations,
        boolean blocked,
        String blockReason,
        boolean fallback,
        boolean servedFromCache,
        EvaluationHarness.EvaluationResult evaluation
) {
}
