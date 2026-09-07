package com.retailco.emailagent.draft;

import com.retailco.emailagent.model.CalendarSlot;
import com.retailco.emailagent.model.EmailMessage;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes the actual reply email, by filling in a fixed template with real
 * information — the original subject line and the specific available
 * times — rather than generating free-form text with an AI model. A
 * production version of this class might ask an LLM to phrase the wording
 * more naturally, but the actual CONTENT (which times, which email
 * thread) would still come from real data, exactly like it does here —
 * never invented.
 * <p>
 * Notice the very last line of the draft always says clearly that it's
 * "not sent, awaiting human review." This class only ever hands back a
 * plain {@code String} — nothing anywhere in this codebase actually sends
 * it anywhere.
 */
public class DraftEngine {

    private static final DateTimeFormatter SLOT_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMM d 'at' h:mm a 'UTC'").withZone(ZoneOffset.UTC);

    public String compose(EmailMessage originalEmail, List<CalendarSlot> candidateSlots) {
        if (candidateSlots == null || candidateSlots.size() < 2) {
            throw new IllegalArgumentException("compose requires at least 2 candidate slots");
        }
        String slotLines = candidateSlots.stream()
                .map(s -> "  - " + SLOT_FORMAT.format(s.start()))
                .collect(Collectors.joining("\n"));

        return "Subject: Re: " + originalEmail.subject() + "\n\n"
                + "Hi,\n\n"
                + "Thanks for reaching out about \"" + originalEmail.subject() + "\". "
                + "I checked the calendar and here are a few times that work:\n\n"
                + slotLines + "\n\n"
                + "Let me know which works best, or suggest another time if none of these fit.\n\n"
                + "[DRAFT -- not sent. Awaiting human review and approval before this reply goes out.]\n";
    }
}
