package com.retailco.bankrag.assistant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The request body for asking a question. An empty query is rejected with
 * a clean 400 error before it ever reaches {@code RagAssistant.ask()},
 * thanks to the {@code @NotBlank} validation below.
 */
public record AskRequest(
        @NotBlank(message = "query is required")
        String query
) {
}
