# ADR-002: Polyglot Persistence — Database-per-Service

**Status:** Accepted
**Date:** 2026-08-30
**Deciders:** Architecture team (AI-assisted, human-reviewed)

## Context

Microservices architecture (ADR-001) requires each service to own its data so that services remain independently deployable and are not coupled through a shared schema. Each functional area has different data-access patterns:

- User Management: transactional, moderate volume, strong consistency required for auth.
- Product Catalog: read-heavy, needs full-text/faceted search, tolerant of brief staleness.
- Cart: ephemeral, high write/read churn per session, does not need to survive indefinitely.
- Order Management: transactional, strong consistency, needs a durable audit trail (status history).
- Payment: transactional, strict consistency, minimal data footprint (must not store raw card data).

## Decision

Adopt **database-per-service** with technology chosen per access pattern rather than one shared database:

| Service | Datastore | Reasoning |
|---|---|---|
| user-management-service | PostgreSQL | ACID guarantees for credentials, roles, addresses |
| product-catalog-service | PostgreSQL (system of record) + Elasticsearch (search index) | Relational integrity for product data; Elasticsearch for fast full-text/faceted search at scale |
| cart-service | Redis | Cart state is ephemeral, key-value shaped, and benefits from TTL expiry and low-latency reads/writes |
| order-management-service | PostgreSQL | Orders require strong consistency and a durable, queryable history |
| payment-service | PostgreSQL (tokenized references only) | Strong consistency for financial records; **no raw card data is stored** — only gateway tokens/references, keeping PCI-DSS scope minimal (SAQ-A) |

## Consequences

**Positive**
- Each team/service can choose the right tool for its access pattern instead of compromising on one shared schema.
- Cart's use of Redis avoids polluting a relational database with high-churn, short-lived data.
- Catalog's Elasticsearch index keeps search fast independent of transactional load on PostgreSQL.

**Negative / trade-offs accepted**
- No cross-service joins or foreign keys — cross-service data (e.g., "show me the product name on an order line") must be resolved via API calls or denormalized snapshots taken at order-creation time.
- Product Catalog now has an eventual-consistency window between PostgreSQL (source of truth) and Elasticsearch (search index), which must be tracked and monitored (see `architecture/architecture.json` risks).
- Requires operating multiple database technologies (PostgreSQL, Redis, Elasticsearch) rather than one, increasing operational surface area.

## Alternatives Considered

| Option | Rejected because |
|---|---|
| Single shared PostgreSQL database for all services | Reintroduces tight coupling that microservices are meant to avoid; a schema migration for Cart would risk locking tables used by Order/Payment. |
| Single NoSQL store (e.g., MongoDB) for everything | Order and Payment need strong relational/ACID guarantees for financial correctness; a single document store is a poor fit for Payment's compliance requirements. |
