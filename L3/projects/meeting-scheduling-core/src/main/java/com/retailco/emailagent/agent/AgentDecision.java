package com.retailco.emailagent.agent;

import com.retailco.emailagent.model.MeetingProposal;

/**
 * What {@link SchedulingAgent} decided for one email, mirroring the
 * sealed-interface decision pattern this submission uses elsewhere (e.g.
 * L2/UC6's {@code AccessPolicy.Decision}) so every outcome is a named,
 * exhaustively-switchable type rather than a boolean plus a nullable
 * field.
 */
public sealed interface AgentDecision {

    record Proposed(MeetingProposal proposal) implements AgentDecision {}

    record NotSchedulingRelated(String reason) implements AgentDecision {}

    record BlockedByGuardrail(String reason) implements AgentDecision {}

    record SkippedDuplicate(String dedupeKey) implements AgentDecision {}

    record NoSlotsAvailable(String reason) implements AgentDecision {}
}
