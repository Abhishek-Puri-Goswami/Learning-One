package com.retailco.bankrag.observability.dto;

import com.retailco.bankrag.assistant.CitationExtractor;
import com.retailco.bankrag.assistant.EvaluationHarness;

import java.util.List;

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
