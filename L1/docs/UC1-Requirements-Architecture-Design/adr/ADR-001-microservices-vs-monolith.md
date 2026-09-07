# ADR-001: Adopt a Microservices Architecture over a Monolith

**Status:** Accepted
**Date:** 2026-08-30
**Deciders:** Architecture team (AI-assisted, human-reviewed)

## Context

The retail company needs an Online Shopping System with five functional areas: Product Catalog, Cart, Order Management, Payment, and User Management. The system must support **10,000 concurrent users**, expose **REST APIs only**, and follow a **contract-first** design approach.

Two architecture options were evaluated using an AI pair-programmer prompt (see `prompt-engineering/prompts-used.md`) constrained to produce a structured comparison:

1. **Monolith** — single deployable Spring Boot application, one shared database.
2. **Microservices** — one independently deployable service per functional area.

## Decision

We will build the system as **microservices**, one service per bounded context: `user-management-service`, `product-catalog-service`, `cart-service`, `order-management-service`, `payment-service`, fronted by an `api-gateway`.

## Rationale

- **Independent scaling under load.** Product Catalog (read-heavy, browsing traffic) and Cart (bursty, session-heavy) have very different scaling profiles than Payment (low-volume, high-criticality). At 10k concurrent users, a monolith would force us to scale the whole application to satisfy the busiest module.
- **Blast radius / isolation.** Payment processing has the strictest compliance and reliability bar (PCI-DSS). Isolating it into its own service limits the impact of a bug or deployment issue in, say, the Catalog service.
- **Independent deployability.** Catalog and Cart will change frequently (promotions, UI experiments); Payment should change rarely and under tighter review. Microservices let each evolve on its own release cadence.
- **Team/ownership boundaries.** Each service maps to a clear business capability, which keeps codebases small enough for a single team (or a single developer completing this use case) to reason about end-to-end.

## Consequences

**Positive**
- Services can be scaled, deployed, and rolled back independently.
- Failure in one service (e.g., Catalog search) does not necessarily take down Checkout.

**Negative / trade-offs accepted**
- No distributed 2-phase-commit transaction across services — checkout must use a **Saga pattern** (event choreography) with compensating actions (see `architecture/architecture.json` → `order-management-service`).
- Network latency and partial-failure handling (timeouts, retries, circuit breakers) must be designed explicitly at every service boundary.
- Operational overhead increases: 6 deployable units instead of 1, each needing its own health checks, logging, and CI/CD pipeline.
- Idempotency keys are required on all state-mutating endpoints (Order creation, Payment authorization) to survive client retries safely — a concern that is trivial in a monolith with a single transaction but essential here.

## Alternatives Considered

| Option | Rejected because |
|---|---|
| Monolith | Cannot scale Catalog/Cart independently from Payment; a bug in one module risks bringing down checkout entirely; conflicts with the contract-first REST requirement for cleanly bounded APIs. |
| Modular monolith (single deployable, internal module boundaries) | Improves code organization but does not solve the independent-scaling requirement at 10k concurrent users; deferred as a possible starting point if the team were larger, but rejected here since the brief explicitly asks for microservice boundaries. |
