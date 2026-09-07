package com.retailco.emailagent.inbox;

import com.retailco.emailagent.model.EmailMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the LLD's `/inbox/latest` tool contract. A real adapter
 * would call Microsoft Graph or the Gmail API; per the HLD's "mock/
 * simulated APIs" support requirement, this is an in-memory list seeded
 * from fixture data, with the exact same field shape ({@code EmailMessage})
 * a real adapter would return -- only the transport differs, disclosed
 * here rather than silently pretending to be a live inbox.
 */
public class InboxTool {

    private final List<EmailMessage> inbox = new ArrayList<>();

    public void seed(EmailMessage message) {
        inbox.add(message);
    }

    /** @return the inbox in receipt order, oldest first -- mirrors `/inbox/latest`'s documented ordering. */
    public List<EmailMessage> getLatest() {
        return List.copyOf(inbox);
    }

    public List<EmailMessage> getLatest(int limit) {
        int from = Math.max(0, inbox.size() - limit);
        return List.copyOf(inbox.subList(from, inbox.size()));
    }
}
