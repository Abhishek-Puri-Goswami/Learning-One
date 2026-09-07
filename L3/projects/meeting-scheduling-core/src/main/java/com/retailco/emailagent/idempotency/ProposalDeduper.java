package com.retailco.emailagent.idempotency;

import com.retailco.emailagent.model.MeetingProposal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes sure we never draft the same meeting proposal twice. It
 * remembers a "dedupe key" (thread, attendees, duration, day — see
 * {@link MeetingProposal#dedupeKey()}) for every proposal we've already
 * made. If the agent happens to run over the same email again — say,
 * after retrying following a temporary failure, or a scheduled re-check
 * of the inbox — this stops it from drafting a second, nearly-identical
 * reply for something it already handled.
 */
public class ProposalDeduper {

    private final Set<String> seenKeys = ConcurrentHashMap.newKeySet();

    /** @return true if this is the first time this proposal's dedupe key has been seen (i.e. it should proceed). */
    public boolean shouldProceed(MeetingProposal proposal) {
        return seenKeys.add(proposal.dedupeKey());
    }

    public int seenCount() {
        return seenKeys.size();
    }
}
