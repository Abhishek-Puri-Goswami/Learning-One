package com.retailco.bankrag.integration.dto;

/**
 * A deliberately generic envelope: `type` names which UnifiedResponse
 * variant fired (POLICY_ANSWER, LIVE_DATA, ACCESS_DENIED, AMBIGUOUS), and
 * `payload` carries that variant's actual content (RagAssistant's
 * AssistantResponse, a masked data list, a denial reason, or a
 * clarification message respectively). Kept as Object rather than four
 * separate optional fields so the response shape doesn't grow a new null
 * field every time a new intent type is added.
 */
public record AskResponse(String type, Object payload) {
}
