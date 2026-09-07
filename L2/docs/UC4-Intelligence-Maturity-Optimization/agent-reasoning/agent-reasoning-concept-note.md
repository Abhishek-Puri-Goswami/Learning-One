# Agent Reasoning Concept Note

Deliverable: "Agent reasoning concept note," per L2 HLD UseCase4 Functional Scope: "Agent reasoning awareness (conceptual)." Explicitly conceptual per the HLD's own parenthetical — this note explains the idea and grounds it in what this submission has actually built, rather than implementing a new agent framework (which belongs to L2/UC5's "Final Integrated Banking RAG System" scope, if anywhere in this submission).

## What "agent reasoning" means here

Everything built across L2/UC1–UC4 so far is a **fixed pipeline**: a query goes through a predetermined sequence of steps (guardrails → retrieval → prompt → generation → citation → trace → evaluation, per L2/UC2's `RagAssistant.ask()`) with no step able to decide to skip, repeat, or reorder any other step based on what it finds along the way. This is appropriate for a first version and is exactly what let every step be independently tested and verified (see every prior use case's `SelfTests.java`).

An **agent**, by contrast, is a system where an LLM itself decides, at each step, what to do next — which tool to call, whether retrieved information is sufficient or needs a follow-up query, whether to ask the user a clarifying question, when to stop. "Reasoning" in this context usually refers to techniques like ReAct (interleaved Reasoning + Acting: the model produces a thought, takes an action, observes the result, and repeats) or plan-and-execute (the model drafts a multi-step plan upfront, then executes and revises it).

## Where this submission already has agent-shaped seams, without being an agent

Two places in this submission are structured so that a future agentic layer could plug in without redesigning the underlying logic:

1. **L2/UC3's `IntentClassifier` + `BankingToolService`** (`tool-calling/tool-calling-design.md`, that use case): the routing decision ("is this a policy question or a live-data question, and if live-data, which tool") is currently made by a keyword heuristic. That design note explicitly identifies this as the swap-in point for a real LLM function-calling decision — which IS a (minimal, single-step) instance of agent reasoning: the model choosing which tool to invoke based on the query.
2. **This use case's `ThresholdTuningExperiment`** (`performance/performance-optimization-summary.md`) is itself a primitive form of what an agent's self-reflection step might do at request time: given a retrieval result, decide "is this confident enough to answer, or should I do something else" — currently a static threshold check, but conceptually the same decision an agentic loop would make dynamically (e.g., "retrieval confidence is low; let me try a reformulated query before giving up").

## What a more agentic version of this system would add

- **Multi-step retrieval**: if the first retrieval's top result is weak (the exact scenario `ThresholdTuningExperiment` studies), an agent could reformulate the query and retrieve again, rather than immediately falling back — turning today's single-shot retrieval-guardrail-or-answer decision into a bounded retry loop.
- **Multi-tool orchestration**: a single user turn like "what's my loan outstanding, and does the current policy manual say anything about prepayment penalties" genuinely needs BOTH `BankingToolService.getLoanOutstanding` (L2/UC3, live data) AND `RagAssistant.ask` (L2/UC2, policy retrieval) — today's `IntentClassifier` explicitly returns `AMBIGUOUS` for a multi-intent query rather than attempting this; an agent would decompose it into two tool calls and compose the results.
- **Self-critique before responding**: an agent could review its own drafted answer against the retrieved context before returning it — a stronger version of this use case's `EvaluationHarness` faithfulness check (L2/UC2), computed at generation time by the model itself rather than after the fact by a lexical-overlap proxy.

## Why this stays conceptual in this submission

Every one of the above requires a real, reachable LLM API capable of multi-step tool-calling — not available in this sandbox (no network egress to any LLM provider, disclosed consistently since L2/UC2's `ExtractiveStubLlmClient`). Building a simulated multi-step agent loop around the extractive stub would produce output that looks agentic without demonstrating anything real about agent reasoning, which this submission has consistently avoided doing elsewhere (preferring real, verified, single-step execution over simulated multi-step behavior). This note exists to show the design is *ready* for that extension — the seams identified above — not to claim it was built.
