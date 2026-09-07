package com.retailco.bankrag.integration.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The request body for {@code POST /api/v1/support/ask}. {@code query}
 * is always required. {@code accountNumber} only matters when the
 * question turns out to be about transaction history, and
 * {@code requestedCustomerId} only matters for any live-data question —
 * both are optional fields here, rather than split into separate
 * endpoints, because which fields actually matter isn't known until
 * {@code IntentClassifier} figures out what kind of question this is.
 */
public record AskRequest(
        @NotBlank(message = "query is required") String query,
        String requestedCustomerId,
        String accountNumber
) {
}
