package com.retailco.emailagent.model;

import java.time.Instant;
import java.util.List;

// CONCEPT: Domain model (record) -- one email, in the same shape a real
// email API would return.
/**
 * Deliverable: "Conceptual Data Model / Email" (L3 HLD section 10; LLD
 * section 8.1's inbox contract). Field names match the LLD's JSON schema
 * example verbatim (`id`, `threadId`, `subject`, `from`, `to`, `cc`,
 * `body`, `receivedAt`) so this is the same wire shape a real Graph/Gmail
 * adapter would produce -- only the transport (mock JSON fixtures here,
 * per HLD's "mock/simulated APIs" support requirement) differs.
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
