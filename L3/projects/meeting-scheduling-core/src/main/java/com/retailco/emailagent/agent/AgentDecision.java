package com.retailco.emailagent.agent;

import com.retailco.emailagent.model.MeetingProposal;

// CONCEPT: Sealed interface -- a fixed, closed set of possible outcomes.
// PURPOSE: Every email the agent processes ends in exactly one of these 5
// outcomes. A `switch` over an AgentDecision can be checked exhaustively
// by the compiler (no forgotten case, no `default` needed), which is
// safer than a boolean flag plus a nullable "reason" field.
public sealed interface AgentDecision {

    record Proposed(MeetingProposal proposal) implements AgentDecision {}

    record NotSchedulingRelated(String reason) implements AgentDecision {}

    record BlockedByGuardrail(String reason) implements AgentDecision {}

    record SkippedDuplicate(String dedupeKey) implements AgentDecision {}

    record NoSlotsAvailable(String reason) implements AgentDecision {}
}
