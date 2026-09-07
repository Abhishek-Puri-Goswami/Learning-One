# ADR-003: AI Must NOT Generate or Control Payment Processing Decision Logic

**Status:** Accepted
**Date:** 2026-08-30
**Deciders:** Architecture team (AI-assisted for drafting, human-approved — this ADR itself is an example of "when NOT to use AI")

## Context

USE CASE 1 explicitly calls out "when NOT to use AI" as a required competency. AI code assistants (Copilot, Amazon Q, ChatGPT-style tools) are used elsewhere in this project to scaffold APIs, generate UI components, review code, and generate tests. The question this ADR answers: **should AI be allowed to author or autonomously modify the payment authorization/capture/refund decision logic in `payment-service`?**

## Decision

**No.** AI assistance is restricted to a supporting role around the payment domain, never the core decision logic itself:

**AI MAY be used for:**
- Generating boilerplate (DTOs, controller scaffolding, OpenAPI specs) that is then reviewed line-by-line by a human before merging.
- Generating unit tests / edge cases for payment code that a human already wrote.
- Reviewing payment code for style, complexity, and known anti-patterns (see L1/UC4).
- Drafting documentation (like this ADR) that a human then approves.

**AI MUST NOT be used to:**
- Autonomously decide whether a transaction is authorized, captured, or refunded.
- Generate the core amount-calculation, currency-conversion, or fraud-scoring logic without deterministic, human-authored, testable rules behind it.
- Directly integrate with the payment gateway's live credentials or make gateway calls without human-reviewed code in the path.
- Be given card data, tokens, or credentials as prompt context.

## Rationale

1. **Hallucination risk is unacceptable in a financial-decision path.** An LLM is a probabilistic system; a payment authorization decision must be deterministic and auditable. A hallucinated API call or logic error here causes direct financial loss, not just a bug.
2. **Regulatory compliance (PCI-DSS).** Payment logic must be traceable to a specific, reviewed, versioned change — "the AI suggested it" is not an acceptable audit trail for a financial regulator.
3. **Liability and accountability.** If a payment is incorrectly charged, refunded, or duplicated, there must be a clear, human-owned decision trail. Autonomous AI decision-making in this path diffuses accountability.
4. **Determinism requirement.** Retry/idempotency logic (see ADR-001) depends on payment operations being deterministic given the same idempotency key. Generative, non-deterministic code paths undermine that guarantee.

## Consequences

- `payment-service` code that touches authorization/capture/refund decisions requires **mandatory human review** even when AI-assisted, with no exceptions in CI/CD (see L2/UC6 for the equivalent CI/CD gating pattern used elsewhere in this project).
- Slower initial development of `payment-service` relative to, e.g., `product-catalog-service`, which is an accepted trade-off.
- This restriction must be documented in the team's AI usage policy and referenced from `payment-service`'s README so future contributors don't silently relax it.

## Alternatives Considered

| Option | Rejected because |
|---|---|
| Allow AI to write payment logic with a human "rubber-stamp" review | In practice, reviewers approve AI output faster and less critically than human-authored code ("automation bias"), which defeats the purpose of the review gate. |
| Full AI autonomy with a post-hoc audit log | Post-hoc auditing does not prevent an incorrect charge from happening in the first place; unacceptable for a financial system. |
