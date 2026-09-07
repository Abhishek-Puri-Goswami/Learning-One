package com.retailco.bankrag.observability.dto;

import com.retailco.bankrag.assistant.CitationExtractor;
import com.retailco.bankrag.assistant.EvaluationHarness;

import java.util.List;

// CONCEPT/PURPOSE: same stable-REST-contract DTO pattern as
// rag-assistant-service's AskResponse, plus one extra field --
// `servedFromCache` -- exposing UC4's caching layer to the API consumer.
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
