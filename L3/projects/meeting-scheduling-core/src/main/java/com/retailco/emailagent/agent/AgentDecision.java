package com.retailco.emailagent.agent;

import com.retailco.emailagent.model.MeetingProposal;

/**
 * Every email our agent processes ends in exactly one of these five
 * outcomes. Using a "sealed interface" here (a fixed, closed list of
 * allowed implementations) means the compiler can check that a
 * {@code switch} over an {@code AgentDecision} handles all five cases —
 * which is safer than using a plain boolean plus a nullable "reason"
 * field, where it's easy to forget to check something.
 */
public sealed interface AgentDecision {

    record Proposed(MeetingProposal proposal) implements AgentDecision {}

    record NotSchedulingRelated(String reason) implements AgentDecision {}

    record BlockedByGuardrail(String reason) implements AgentDecision {}

    record SkippedDuplicate(String dedupeKey) implements AgentDecision {}

    record NoSlotsAvailable(String reason) implements AgentDecision {}
}
