package com.retailco.emailagent.draft;

import com.retailco.emailagent.model.CalendarSlot;
import com.retailco.emailagent.model.EmailMessage;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

// CONCEPT: Template-based text generation (no LLM call) -- builds a reply
// by filling in a fixed template with real data (subject, candidate
// times), rather than generating free-form text.
// IMPORTANT: the draft is explicitly labeled "not sent, awaiting human
// approval" -- this class only ever produces a String; nothing in this
// codebase ever sends it (see ToolAllowlist).
/**
 * Template-based reply composition -- no LLM call (this sandbox has no LLM
 * API access; a production version would prompt an LLM to phrase this
 * more naturally, but the *content* -- which slots, which thread -- would
 * be assembled the same way this class does it, from real tool output,
 * not invented). LLD Stage 1 acceptance criterion: "accurate context
 * reference" -- this class quotes the original subject and the specific
 * candidate times back to the sender, rather than a generic template with
 * no connection to the actual email.
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
