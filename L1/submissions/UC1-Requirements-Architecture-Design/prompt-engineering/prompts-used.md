# Prompt Engineering Notes — L1 UC1

## 1. Intent vs. Instructions

A **bad prompt** states intent only and leaves the model to guess structure, constraints, and format. A **good prompt** gives the model a role, explicit constraints, and a required output shape — turning intent into an executable instruction set.

### Bad Prompt (intent only)
```
Design an online shopping system.
```
Problems: no scale target, no API style, no output format — the model will produce a generic, unverifiable essay-style answer that can't be validated or fed into the next pipeline step.

### Good Prompt (context + constraints + structure) — used to generate `architecture/architecture.json`
```
You are a senior solution architect.
Design a microservices-based online shopping system.
Constraints:
- Must support 10k concurrent users
- REST APIs only
- Use contract-first design
- Output in structured JSON with: services, responsibilities, API boundaries, risks.
```
This is the exact prompt template supplied in USE CASE 1, used as the basis for `architecture/architecture.json`.

## 2. Temperature Control

**Temperature used: 0.2 (low).**

Rationale:
- This task produces a **structured artifact** (JSON architecture spec, ADRs) that downstream steps (L1/UC2 API scaffolding) depend on. High temperature (e.g., 0.8–1.0) increases lexical creativity but also increases the chance of inconsistent field names, invented services, or malformed JSON — all of which would break the contract-first pipeline.
- Low temperature (0.0–0.3) favors the model's highest-probability, most "conventional" architecture patterns (e.g., standard microservice boundaries, standard REST verbs), which is exactly what's wanted for a foundational design document that a human will review and correct, not a creative brainstorm.
- Temperature was intentionally **not** set to 0.0: a small amount of variability (0.2) was kept so that if the JSON needed to be regenerated (e.g., after adding a constraint), the model wasn't rigidly locked into a single deterministic phrasing that might miss an edge case a second pass could catch.

When a **higher temperature (0.6–0.8)** would be more appropriate instead: open-ended brainstorming of *alternative* architectures to compare (the "Alternatives Considered" sections in the ADRs), where more divergent options are actually useful before a decision is locked in.

## 3. Token / Context-Length Control

- **Context window budgeting:** the source use-case brief plus prior use case's output (business requirements) were kept under ~2k tokens of input context so the model's attention stays focused on this task rather than being diluted across the full L1 curriculum (~15k+ tokens across all 5 use cases and reference PDFs).
- **Chunking large source docs:** where a reference document is large (e.g., the "Instructional Lab Guide" or the AI Assistant Use Cases showcase PDFs), only the relevant section was extracted and passed in — the same chunking discipline used later in the L2 RAG use cases — rather than pasting entire multi-page PDFs into the prompt.
- **Output token budget:** the required JSON output (services, responsibilities, API boundaries, risks) was capped implicitly by asking for exactly those four fields per service, preventing the model from padding the response with prose and running into a truncated/incomplete JSON object.
- **Structured-output validation:** after generation, the JSON was validated for syntactic correctness (`python -m json.tool`) and checked that every service listed in `architecture.json` also appears in the technical requirements table — a manual "does this parse, does this map to requirements" gate before accepting the AI output.

## 4. "When NOT to Use AI" (see ADR-003)

This use case's brief explicitly lists "When NOT to use AI" as a topic to cover. The concrete example produced in this project is **ADR-003**: AI is not permitted to author or autonomously control payment authorization/capture/refund decision logic, only supporting artifacts (docs, tests, boilerplate) around it.
