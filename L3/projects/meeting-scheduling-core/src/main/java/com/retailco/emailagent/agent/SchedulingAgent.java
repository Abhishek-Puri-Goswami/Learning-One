package com.retailco.emailagent.agent;

import com.retailco.emailagent.calendar.CalendarTool;
import com.retailco.emailagent.draft.DraftEngine;
import com.retailco.emailagent.guardrails.PromptInjectionGuard;
import com.retailco.emailagent.idempotency.ProposalDeduper;
import com.retailco.emailagent.inbox.InboxTool;
import com.retailco.emailagent.intent.EmailIntent;
import com.retailco.emailagent.intent.IntentClassifier;
import com.retailco.emailagent.logging.PiiRedactingLogger;
import com.retailco.emailagent.model.CalendarSlot;
import com.retailco.emailagent.model.EmailMessage;
import com.retailco.emailagent.model.MeetingProposal;
import com.retailco.emailagent.nlp.DateTimeExtractor;
import com.retailco.emailagent.nlp.TimeHint;
import com.retailco.emailagent.observability.TraceLogger;
import com.retailco.emailagent.resilience.RetryingExecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// CONCEPT: AI Agent orchestrator -- the class to read FIRST to understand
// this whole module. An "agent" here just means: read input (emails),
// decide what to do (classify intent), call tools (calendar lookup), and
// produce an action (a draft reply) -- all coordinated by one class.
//
// FLOW (see processOne() below, step by step):
// 1. Classify the email's intent. Not a scheduling request? Stop here.
// 2. Check the email body for prompt-injection attempts (a guardrail) --
//    blocked? Stop here.
// 3. Extract a rough time hint from the email text (e.g. "Tuesday
//    afternoon").
// 4. Ask the calendar tool for available slots (with automatic retry).
// 5. Fewer than 2 slots found? Stop here.
// 6. Compose a draft reply referencing the top slots.
// 7. Check the dedupe guard so the same proposal isn't drafted twice.
// 8. Return a Proposed decision -- a DRAFT only.
//
// IMPORTANT: this agent NEVER sends an email or books a calendar slot on
// its own -- every path either stops early or ends in a draft awaiting
// human approval. There is no "send" tool available to it at all (see
// ToolAllowlist), so this is a safety property enforced by what tools
// exist, not just by how this class happens to be written.
//
// WHY every step calls traceLogger/auditLog: this makes the agent's
// reasoning inspectable after the fact -- you can see exactly why it did
// or didn't act on a given email, which matters a lot for an autonomous
// system making decisions on someone's behalf.
public class SchedulingAgent {

    private static final Duration DEFAULT_MEETING_DURATION = Duration.ofMinutes(30);

    private final InboxTool inbox;
    private final IntentClassifier intentClassifier;
    private final DateTimeExtractor dateTimeExtractor;
    private final CalendarTool calendarTool;
    private final DraftEngine draftEngine;
    private final PromptInjectionGuard promptInjectionGuard;
    private final ProposalDeduper deduper;
    private final RetryingExecutor retryingExecutor;
    private final TraceLogger traceLogger;
    private final PiiRedactingLogger auditLog;

    public SchedulingAgent(InboxTool inbox, IntentClassifier intentClassifier, DateTimeExtractor dateTimeExtractor,
                            CalendarTool calendarTool, DraftEngine draftEngine, PromptInjectionGuard promptInjectionGuard,
                            ProposalDeduper deduper, RetryingExecutor retryingExecutor, TraceLogger traceLogger,
                            PiiRedactingLogger auditLog) {
        this.inbox = inbox;
        this.intentClassifier = intentClassifier;
        this.dateTimeExtractor = dateTimeExtractor;
        this.calendarTool = calendarTool;
        this.draftEngine = draftEngine;
        this.promptInjectionGuard = promptInjectionGuard;
        this.deduper = deduper;
        this.retryingExecutor = retryingExecutor;
        this.traceLogger = traceLogger;
        this.auditLog = auditLog;
    }

    public List<AgentDecision> processInbox() {
        List<AgentDecision> decisions = new ArrayList<>();
        for (EmailMessage email : inbox.getLatest()) {
            decisions.add(processOne(email));
        }
        return decisions;
    }

    public AgentDecision processOne(EmailMessage email) {
        traceLogger.log(email.threadId(), "intent_classification_started", "subject=" + email.subject());
        EmailIntent intent = intentClassifier.classify(email);

        if (intent != EmailIntent.SCHEDULING_REQUEST) {
            traceLogger.log(email.threadId(), "not_scheduling_related", "");
            auditLog.log("email from " + email.from() + " classified as non-scheduling"); // from() redacted by auditLog
            return new AgentDecision.NotSchedulingRelated("intent classifier returned " + intent);
        }

        if (promptInjectionGuard.containsSuspiciousInstruction(email.body())) {
            traceLogger.log(email.threadId(), "blocked_by_guardrail", "prompt-injection pattern detected");
            auditLog.log("email from " + email.from() + " blocked: suspected prompt injection");
            return new AgentDecision.BlockedByGuardrail("email body contains a suspected prompt-injection pattern");
        }

        TimeHint hint = dateTimeExtractor.extract(email.body());
        traceLogger.log(email.threadId(), "time_hint_extracted",
                "dayOfWeek=" + hint.dayOfWeek() + " partOfDay=" + hint.partOfDay() + " explicitTime=" + hint.explicitTime());

        List<String> participants = participantsOf(email);
        List<CalendarSlot> slots = retryingExecutor.executeWithRetry(
                () -> calendarTool.findAvailableSlots(participants, DEFAULT_MEETING_DURATION, hint));
        traceLogger.log(email.threadId(), "calendar_lookup_completed", "slotsFound=" + slots.size());

        if (slots.size() < 2) {
            traceLogger.log(email.threadId(), "no_slots_available", "");
            return new AgentDecision.NoSlotsAvailable("fewer than 2 candidate slots found for " + participants);
        }

        List<CalendarSlot> topTwo = slots.subList(0, Math.min(3, slots.size()));
        String draftBody = draftEngine.compose(email, topTwo);
        MeetingProposal proposal = new MeetingProposal(email.threadId(), participants, DEFAULT_MEETING_DURATION,
                topTwo, draftBody);

        if (!deduper.shouldProceed(proposal)) {
            traceLogger.log(email.threadId(), "skipped_duplicate", proposal.dedupeKey());
            return new AgentDecision.SkippedDuplicate(proposal.dedupeKey());
        }

        traceLogger.log(email.threadId(), "draft_composed", "slotsOffered=" + topTwo.size());
        auditLog.log("drafted a scheduling reply for thread " + email.threadId() + " to " + email.from()
                + " -- awaiting human approval, not sent");
        return new AgentDecision.Proposed(proposal);
    }

    private List<String> participantsOf(EmailMessage email) {
        Set<String> all = new LinkedHashSet<>();
        all.add(email.from());
        all.addAll(email.to());
        all.addAll(email.cc());
        return List.copyOf(all);
    }
}
