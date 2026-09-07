package com.retailco.emailagent.model;

import java.time.Duration;
import java.util.List;

/**
 * What our agent produces for one scheduling email: the candidate meeting
 * times it found, plus the drafted reply mentioning them. The constructor
 * below insists on at least 2 candidate slots — fewer than that isn't a
 * useful proposal to offer someone.
 * <p>
 * Notice there is no "sent" field anywhere on this record — only a draft.
 * That's deliberate: this type simply has no way to represent "an email
 * that got sent," because this agent never sends anything.
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
