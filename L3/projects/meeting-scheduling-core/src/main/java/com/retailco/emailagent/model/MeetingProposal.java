package com.retailco.emailagent.model;

import java.time.Duration;
import java.util.List;

/**
 * The agent's output for one scheduling-intent email: the candidate slots
 * it found (LLD acceptance criterion: at least 2 valid slots) and the
 * drafted reply referencing them. Never sent automatically -- see
 * {@code guardrails.GuardrailChecker} and {@code draft.DraftEngine}'s
 * Javadoc for the human-approval-gate rule this record's existence
 * enforces structurally: there is no field here for "sent," only "drafted."
 */
public record MeetingProposal(
        String threadId,
        List<String> attendees,
        Duration duration,
        List<CalendarSlot> candidateSlots,
        String draftReplyBody
) {

    public MeetingProposal {
        if (candidateSlots == null || candidateSlots.size() < 2) {
            throw new IllegalArgumentException(
                    "a MeetingProposal must offer at least 2 candidate slots (LLD Stage 1 acceptance criterion), got: "
                            + (candidateSlots == null ? 0 : candidateSlots.size()));
        }
        attendees = List.copyOf(attendees);
        candidateSlots = List.copyOf(candidateSlots);
    }

    /** The dedupe key from the LLD's idempotency rule: (threadId, attendees, duration, day). */
    public String dedupeKey() {
        String day = candidateSlots.get(0).start().toString().substring(0, 10); // yyyy-MM-dd, UTC
        return threadId + "|" + String.join(",", attendees.stream().sorted().toList())
                + "|" + duration + "|" + day;
    }
}
