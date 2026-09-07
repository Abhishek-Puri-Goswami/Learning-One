package com.retailco.bankrag.service.dto;

import java.util.List;

// CONCEPT: Response DTO carrying both the search results AND guardrail
// metadata in one shape.
// PURPOSE: `guardrailTriggered`/`guardrailMessage` let a caller (a
// frontend, or another AI agent) know when a search result should NOT be
// trusted as a confident answer, rather than silently returning weak
// results indistinguishable from strong ones. `topScore`/`scoreMargin`
// are exposed raw so a caller could apply its OWN confidence policy
// instead of only trusting this service's guardrail decision.
public record SearchResponse(
        String query,
        String method,
        List<SearchResultItem> results,
        boolean guardrailTriggered,
        String guardrailMessage,
        Double topScore,
        Double scoreMargin
) {
}
