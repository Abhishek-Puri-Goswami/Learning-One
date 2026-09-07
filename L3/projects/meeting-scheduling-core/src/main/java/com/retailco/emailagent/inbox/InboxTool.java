package com.retailco.emailagent.inbox;

import com.retailco.emailagent.model.EmailMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Another "AI tool" — this one gives the agent read access to emails,
 * without the agent needing to care whether they're coming from a simple
 * in-memory list (like here) or a real mailbox API. A real version of
 * this class would call something like Microsoft Graph or the Gmail API,
 * but would return the exact same {@code EmailMessage} shape, so nothing
 * calling this class would need to change.
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
