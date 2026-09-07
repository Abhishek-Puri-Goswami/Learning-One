package com.retailco.bankrag.assistant.dto;

import com.retailco.bankrag.assistant.CitationExtractor;
import com.retailco.bankrag.assistant.EvaluationHarness;

import java.util.List;

// CONCEPT: Response DTO -- deliberately a separate type from
// RagAssistant.AssistantResponse even though the fields currently match.
// WHY: this is the API's stable public contract; RagAssistant's internal
// return type can be refactored freely as long as AskController keeps
// mapping it onto this same AskResponse shape (see AskController.ask()).
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
