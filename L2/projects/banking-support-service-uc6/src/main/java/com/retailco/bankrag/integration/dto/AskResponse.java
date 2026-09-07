package com.retailco.bankrag.integration.dto;

/**
 * One shared response shape that can represent any of the four possible
 * outcomes from {@code /ask}. {@code type} names which outcome happened
 * ({@code POLICY_ANSWER}, {@code LIVE_DATA}, {@code ACCESS_DENIED}, or
 * {@code AMBIGUOUS}), and {@code payload} carries that outcome's actual
 * content. Keeping {@code payload} as a generic {@code Object} — instead
 * of four separate optional fields, one per outcome — means the response
 * shape doesn't grow a new always-empty field every time a new kind of
 * outcome is added later.
 */
public record AskResponse(String type, Object payload) {
}
