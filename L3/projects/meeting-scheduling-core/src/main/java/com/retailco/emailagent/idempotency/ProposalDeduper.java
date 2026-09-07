package com.retailco.emailagent.idempotency;

import com.retailco.emailagent.model.MeetingProposal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// CONCEPT: Idempotency guard -- prevents the same action from happening
// twice. Here: don't draft the same meeting proposal again if it's
// already been drafted (e.g. the agent re-runs over the same inbox).
/**
 * Per the LLD's idempotency rule: dedupe meeting proposals by
 * {@code (threadId, attendees, duration, day)} -- see
 * {@link MeetingProposal#dedupeKey()}. Prevents the agent from drafting a
 * second, near-identical proposal for the same thread if it's re-run
 * against an inbox that hasn't changed (e.g. a retry after a transient
 * failure, or a scheduled re-poll that re-reads an already-handled
 * email) -- the LLD's stated failure mode this rule exists to prevent.
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
