# L3 / Stage 1 — Single Agent: Meeting Scheduling

Gen-AI use case submission. Per the L3 HLD/LLD's Stage 1 scope: a single agent that reads an inbox, recognizes a scheduling-intent email, finds real calendar availability, and drafts (never sends) a reply proposing at least 2 candidate times, referencing the original email's actual context.

## What's real vs. documented-but-unverified

Same disclosure standard as L1/L2: this module is **pure JDK, zero external dependencies** -- no LLM API, no real email/calendar provider (both blocked or unavailable in this sandbox) -- so every component here is a real, compiled, hand-tested implementation of the *logic* the HLD/LLD describe, with the specific stand-ins named below rather than left implicit.

| Component | What it is here | What a production version would be |
|---|---|---|
| `inbox.InboxTool` | An in-memory list, seeded from fixture data | Microsoft Graph or Gmail API adapter (same `EmailMessage` shape) |
| `intent.IntentClassifier` | Rule-based keyword/phrase matching | An LLM call, prompted to classify intent |
| `nlp.DateTimeExtractor` | A deliberately conservative regex/keyword extractor (day-of-week, morning/afternoon/evening, explicit "2pm"-style times) -- returns an empty hint rather than a wrong guess for anything outside that bounded set | An LLM or a dedicated NL date-parsing library (e.g. duckling) |
| `calendar.CalendarTool` | An in-memory free/busy store + slot search over business hours | Microsoft Graph's or Google Calendar's freebusy API |
| `draft.DraftEngine` | Template-based composition, quoting the real subject and real candidate slots | An LLM call for more natural phrasing, still fed the same real tool output |
| `observability.TraceLogger` | Hand-rolled JSONL, same pattern as L2/UC2's LangSmith stand-in | LangSmith or an equivalent tracing platform |

None of this is presented as more sophisticated than it is -- every class's Javadoc says plainly what it stands in for and why.

## Real, compiled, tested -- not "hand-traced"

Unlike L1's original submission (fixed in that project's own rework), this use case was built to the compiled-and-tested standard from the start:

```bash
cd meeting-scheduling-core
javac -Xlint:all -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.retailco.emailagent.selftest.SelfTests   # 18/18, see reports/selftests-run-log.txt
java -cp out com.retailco.emailagent.demo.Main             # see reports/demo-run-log.txt and reports/trace-log.jsonl
```

## Acceptance criteria, traced to real evidence

| Stage 1 acceptance criterion (per the LLD) | How it's enforced | Proven by |
|---|---|---|
| At least 2 valid candidate slots offered | `CalendarTool.findAvailableSlots` widens its search window automatically if fewer than 2 free slots are found, and `MeetingProposal`'s constructor throws if given fewer than 2 -- structurally impossible to propose only 1 | `testCalendarToolReturnsAtLeastTwoSlotsWhenAvailable`, `testAgentEndToEndSchedulingRequestProducesProposalWithAtLeastTwoSlots` |
| Accurate context reference | `DraftEngine.compose` quotes the original email's real subject line and the real candidate slot times -- not a generic template | See `reports/demo-run-log.txt`'s drafted replies, which name the actual subjects ("Quick sync on Q3 budget", "Vendor call") |
| Human approval gate (draft-only, never auto-sent) | No `send`/`dispatch` method exists anywhere in `SchedulingAgent` or the tools it calls -- structural, not a runtime flag (see `guardrails.ToolAllowlist`'s Javadoc) | `testAgentNeverExposesASendCapability` -- reflectively asserts no such method exists on the class at all |
| Idempotency: dedupe by (threadId, attendees, duration, day) | `idempotency.ProposalDeduper` + `MeetingProposal.dedupeKey()`, exactly the LLD's stated key | `testProposalDeduperBlocksSecondIdenticalProposal` |
| Prompt-injection defense | `guardrails.PromptInjectionGuard` scans the email body for known injection patterns before any tool call uses that body's content as anything other than scheduling signal | `testPromptInjectionGuardDetectsKnownPatterns`, `testAgentBlocksPromptInjectionAttempt` -- see `reports/demo-run-log.txt`'s 4th email, blocked exactly as intended |
| Allowlisted tool actions | `guardrails.ToolAllowlist` names the only 3 actions the agent ever takes; every tool call in `SchedulingAgent` is a direct compile-time method call, not a dynamic dispatcher | Structural (see the enum's own Javadoc) |
| PII redaction in logs | `logging.PiiRedactingLogger` is the only path anything reaches a log line through, and it always redacts email addresses first | `testPiiRedactingLoggerRedactsEmailAddresses` |
| Resilience (retry on transient tool failure) | `resilience.RetryingExecutor` wraps the calendar lookup with linear-backoff retry | `testRetryingExecutorSucceedsAfterTransientFailures`, `testRetryingExecutorThrowsAfterExhaustingAttempts` |

## Reports (real output from this submission's own run)

- `reports/javac-lint-log.txt` -- `javac -Xlint:all`, clean (1 benign `serialVersionUID` warning)
- `reports/selftests-run-log.txt` -- 18/18 tests passing
- `reports/demo-run-log.txt` -- a 4-email narrative run: a happy-path proposal, a correctly-skipped unrelated email, a second happy-path proposal, and a correctly-blocked prompt-injection attempt
- `reports/trace-log.jsonl` -- the real per-step JSONL trace produced by that same demo run
