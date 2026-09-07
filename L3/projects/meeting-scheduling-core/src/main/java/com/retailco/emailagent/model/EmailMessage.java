package com.retailco.emailagent.model;

import java.time.Instant;
import java.util.List;

/**
 * One email, in the same shape a real email API (like Gmail or Microsoft
 * Graph) would actually return — id, thread id, subject, sender,
 * recipients, body, and when it arrived.
 */
public record EmailMessage(
        String id,
        String threadId,
        String subject,
        String from,
        List<String> to,
        List<String> cc,
        String body,
        Instant receivedAt
) {
}
