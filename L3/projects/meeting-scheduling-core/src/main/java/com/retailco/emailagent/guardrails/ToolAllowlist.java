package com.retailco.emailagent.guardrails;

import java.util.Set;

/**
 * This lists every action our agent is allowed to take: read the inbox,
 * check calendar availability, and draft a reply. Just as important as
 * what's on this list is what's NOT — there's no "send email" or "modify
 * calendar" option here at all.
 * <p>
 * This is a common safety idea for AI agents: instead of only telling the
 * agent what it SHOULD do, you limit what it's even CAPABLE of doing.
 * In this codebase, the agent never calls a generic "run this action"
 * method — it only ever calls specific, fixed methods like
 * {@code inbox.getLatest()} or {@code calendar.findAvailableSlots(...)}.
 * This enum exists to write that safety boundary down clearly in one
 * place, as documentation and as something future code changes can be
 * checked against.
 */
public enum ToolAllowlist {
    READ_INBOX,
    READ_CALENDAR_AVAILABILITY,
    DRAFT_REPLY; // never SEND_EMAIL, never MODIFY_CALENDAR -- draft-only per the human-approval-gate rule

    public static boolean isAllowed(ToolAllowlist action) {
        return Set.of(values()).contains(action);
    }
}
