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

/**
 * The single-agent orchestrator for L3 Stage 1 ("Single Agent - Meeting
 * Scheduling"). Per the LLD: read the inbox, classify each email, and for
 * a scheduling-intent email, find at least 2 candidate slots and draft a
 * reply referencing them -- never send it (see
 * {@code guardrails.ToolAllowlist}'s Javadoc: there is no send capability
 * anywhere in this class or the tools it calls). Every step is traced and
 * every guardrail decision is logged through the PII-redacting logger.
 */
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
