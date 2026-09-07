package com.retailco.bankrag.assistant.dto;

import com.retailco.bankrag.assistant.CitationExtractor;
import com.retailco.bankrag.assistant.EvaluationHarness;

import java.util.List;

/**
 * The JSON shape we send back for an answered question. This is
 * deliberately a separate type from {@code RagAssistant.AssistantResponse},
 * even though their fields currently match — this record is the API's
 * stable public contract, so the internal class can keep changing freely
 * as long as {@code AskController} keeps mapping it onto this shape.
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
