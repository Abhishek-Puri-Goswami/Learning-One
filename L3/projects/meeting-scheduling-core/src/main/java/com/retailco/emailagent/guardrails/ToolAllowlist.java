package com.retailco.emailagent.guardrails;

import java.util.Set;

// CONCEPT: Guardrail via an allowlist enum -- documents exactly which
// actions the agent is permitted to take (read inbox, read calendar,
// draft a reply) and, just as importantly, which it is NOT (send email,
// modify calendar). This is a safety pattern common to AI agents: limit
// what the agent CAN do, don't just tell it what it SHOULD do.
/**
 * Per the HLD's "allowlisted tool actions" guardrail: the agent may only
 * invoke a fixed, named set of read/draft actions -- never a generic
 * "execute this" capability. This is enforced structurally, not just by
 * policy: {@code agent.SchedulingAgent} has no method that takes an
 * arbitrary action name and dispatches it; every tool call in this
 * codebase is a direct, compile-time method call
 * ({@code inbox.getLatest()}, {@code calendar.findAvailableSlots(...)},
 * {@code draft.compose(...)}). This enum exists so that structural
 * guarantee is also machine-checkable and documented in one place, and so
 * a future actions-as-data dispatcher (if one is ever added) has
 * something to check against instead of re-deriving the allowlist.
 */
public enum ToolAllowlist {
    READ_INBOX,
    READ_CALENDAR_AVAILABILITY,
    DRAFT_REPLY; // never SEND_EMAIL, never MODIFY_CALENDAR -- draft-only per the human-approval-gate rule

    public static boolean isAllowed(ToolAllowlist action) {
        return Set.of(values()).contains(action);
    }
}
