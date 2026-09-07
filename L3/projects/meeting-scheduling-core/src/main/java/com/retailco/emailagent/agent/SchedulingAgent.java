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
 * This is the heart of the whole module — read this class first to
 * understand how everything fits together. An "AI agent," in the simplest
 * sense, is just something that: reads its input (here, emails), decides
 * what to do about it (classifies the request), calls out to tools to get
 * real information (checks the calendar), and produces an action (drafts
 * a reply). This class is what coordinates all of that.
 * <p>
 * Here's exactly what happens, step by step, inside {@link #processOne}:
 * <ol>
 *   <li>Figure out what kind of email this is. If it's not a scheduling
 *       request, we stop right here.</li>
 *   <li>Check the email's text for anything that looks like an attempt to
 *       trick the agent into doing something it shouldn't (a "guardrail").
 *       If something suspicious is found, we stop.</li>
 *   <li>Pull out a rough hint about when the sender wants to meet (like
 *       "Tuesday afternoon").</li>
 *   <li>Ask the calendar tool for available time slots, automatically
 *       retrying if the call fails.</li>
 *   <li>If fewer than two slots are available, we stop — there's nothing
 *       good to offer.</li>
 *   <li>Write a draft reply that mentions the best available slots.</li>
 *   <li>Check we haven't already drafted this exact same proposal before,
 *       so we don't send the person two nearly-identical drafts.</li>
 *   <li>Return a "Proposed" result — just a DRAFT, nothing more.</li>
 * </ol>
 * <p>
 * A very important safety property: this agent can NEVER send an email or
 * book a meeting on its own. Every path through this method either stops
 * early or ends in a draft that's still waiting for a human to approve
 * it. That's not just a promise made in this comment — there is literally
 * no "send" capability available anywhere in this code for the agent to
 * call (see {@code ToolAllowlist}).
 * <p>
 * You'll also notice every step logs what it's doing. That's on purpose:
 * it means we can always look back afterward and see exactly why the
 * agent did (or didn't) act on a particular email — important for
 * anything that makes decisions automatically on someone's behalf.
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
