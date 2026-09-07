package com.retailco.bankrag.service.dto;

import java.util.List;

/**
 * Carries both the search results AND some guardrail information in one
 * response. {@code guardrailTriggered}/{@code guardrailMessage} let
 * whoever's calling this know when a result should NOT be trusted as
 * confident, instead of silently mixing weak results in with strong
 * ones. {@code topScore}/{@code scoreMargin} are also exposed raw, so a
 * caller could apply its own confidence rules instead of only trusting
 * ours.
 */
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
