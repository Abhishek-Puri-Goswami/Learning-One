# L1 — USE CASE 1: AI-Assisted Requirements & Architecture Design

Online Shopping System (retail company) — Product Catalog, Cart, Order Management, Payment, User Management.

## Deliverables Checklist (per the use case brief)

- [x] **AI-generated architecture JSON** → [`architecture/architecture.json`](architecture/architecture.json)
- [x] **AI-generated architecture diagram (logical)** → [`architecture/architecture-diagram.md`](architecture/architecture-diagram.md)
- [x] **ADR document(s)** → [`adr/`](adr/)
  - `ADR-001-microservices-vs-monolith.md` — why microservices
  - `ADR-002-database-model-selection.md` — recommended database model (polyglot persistence)
  - `ADR-003-no-ai-in-payment-processing-logic.md` — why NOT to use AI in payment processing logic
- [x] **Requirement traceability document** → [`requirements/requirement-traceability-matrix.md`](requirements/requirement-traceability-matrix.md) (plus `business-requirements.md` and `technical-requirements.md`)
- [x] **Explanation of temperature & token control used** → [`prompt-engineering/prompts-used.md`](prompt-engineering/prompts-used.md) (also includes the bad-vs-good prompt example and the intent-vs-instructions discussion)

## Folder Structure

```
UC1-Requirements-Architecture-Design/
├── README.md                              (this file)
├── architecture/
│   ├── architecture.json                  Structured AI output: services, responsibilities, API boundaries, risks
│   └── architecture-diagram.md            Logical architecture diagram (Mermaid)
├── adr/
│   ├── ADR-001-microservices-vs-monolith.md
│   ├── ADR-002-database-model-selection.md
│   └── ADR-003-no-ai-in-payment-processing-logic.md
├── requirements/
│   ├── business-requirements.md
│   ├── technical-requirements.md
│   └── requirement-traceability-matrix.md
└── prompt-engineering/
    └── prompts-used.md                    Bad vs good prompt, temperature/token rationale
```

## How This Feeds the Next Use Cases

- The service boundaries and API endpoints in `architecture.json` are the direct input to **L1/UC2 (Backend API Scaffolding)**, which turns each service's `api_boundaries` into an OpenAPI 3.0 contract.
- The React frontend in **L1/UC3** consumes the same endpoint contracts.
- **ADR-003** (no AI in payment logic) is enforced as a review gate referenced again in **L1/UC4 (Code Review)**.
