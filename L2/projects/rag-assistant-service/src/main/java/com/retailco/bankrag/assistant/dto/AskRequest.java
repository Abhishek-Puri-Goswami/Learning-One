package com.retailco.bankrag.assistant.dto;

import jakarta.validation.constraints.NotBlank;

// CONCEPT: Request DTO with Bean Validation (same @NotBlank + @Valid
// pattern as rag-service's IngestRequest) -- an empty query is rejected
// with a 400 before it ever reaches RagAssistant.ask().
public record AskRequest(
        @NotBlank(message = "query is required")
        String query
) {
}
