package com.retailco.bankrag.integration.dto;

import jakarta.validation.constraints.NotBlank;

// CONCEPT: Request DTO with optional fields whose relevance depends on
// runtime routing -- a deliberate one-endpoint design (see SupportController).
/**
 * Deliverable: "Fully functional AI Banking Support System" as one REST
 * contract. accountNumber is only required when the query resolves to a
 * TRANSACTION_HISTORY intent; requestedCustomerId is only required for any
 * LIVE_DATA intent -- both are optional here rather than split into
 * separate endpoints, since routing (and therefore which fields matter)
 * isn't known until IntentClassifier runs inside the service.
 */
public record AskRequest(
        @NotBlank(message = "query is required") String query,
        String requestedCustomerId,
        String accountNumber
) {
}
