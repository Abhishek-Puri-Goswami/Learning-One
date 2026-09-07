# L3 — Email Drafting / Task-Extraction Agent (2 cumulative stages)

Like L2, L3 is one continuously-scoped project rather than independent exercises: an agentic system that reads an inbox and helps with scheduling and task follow-up, built in two stages. Stage 1 is a single agent (meeting scheduling); Stage 2 adds a second agent (task extraction) plus a supervisor that routes between them. This repo captures that build-up as real git history: one branch per stage, merged into `main` after each.

## Stages

| # | Stage | Branch (merged) | Adds |
|---|---|---|---|
| 1 | Single Agent — Meeting Scheduling | `stage1-single-agent-meeting-scheduling` | `meeting-scheduling-core`: inbox/calendar tools, intent classification, date/time extraction, calendar-slot search, draft composition, guardrails (prompt-injection defense, tool allowlisting), idempotency, resilience, PII-redacted logging, and a JSONL trace log |
| 2 | Multi-Agent — Task Extraction + Supervisor | `stage2-multiagent-task-extraction` | *(not yet built)* |

Run `git log --all --graph --oneline` to see the history so far.

## What's real vs. documented-but-unverified

This project has no LLM API access and no real email/calendar provider reachable from this sandbox, so every agent capability is implemented as real, compiled, pure-JDK logic standing in for what an LLM call or a live Graph/Gmail/Calendar API would do in production -- each stand-in is named explicitly in its class's own Javadoc and in that stage's README, not left implicit. Unlike L1's original submission, this project was built to the compiled-and-tested standard from the start (no Maven/Spring layer here to be blocked by Maven Central in the first place -- this is a plain-JDK project end to end).

See each stage's own README for its specific real/unverified breakdown and reports.
