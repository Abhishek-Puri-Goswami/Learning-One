# Requirement Traceability Matrix — L1 UC1

Traces each business requirement through technical requirements, the owning service/component, the architecture decision that justifies the design, and how it will be verified in later use cases.

| Business Req | Technical Req(s) | Service / Component | Architecture Decision | Verified In |
|---|---|---|---|---|
| BR-01 Browse & search catalog | TR-01, TR-14 | product-catalog-service | ADR-002 (PostgreSQL + Elasticsearch) | L1/UC2 (API scaffold), L1/UC5 (tests) |
| BR-02 Cart management | TR-02, TR-03 | cart-service, product-catalog-service | ADR-002 (Redis for ephemeral cart state) | L1/UC3 (frontend cart UI), L1/UC5 (tests) |
| BR-03 Place & track orders | TR-04, TR-05, TR-11 | order-management-service | ADR-001 (Saga over 2PC) | L1/UC5 (concurrency tests) |
| BR-04 Secure payment | TR-06, TR-07 | payment-service | ADR-003 (no AI in payment decision logic) | L1/UC5 (payment timeout/edge case tests) |
| BR-05 Account management | TR-08, TR-09 | user-management-service | ADR-002 (PostgreSQL for auth data) | L1/UC5 (tests) |
| BR-06 Availability under peak load | TR-10, TR-11, TR-14 | api-gateway, all services | ADR-001 (independent scaling) | L2/UC4 (performance/observability, analogous pattern) |
| BR-07 Data protection & compliance | TR-03, TR-06, TR-09, TR-12 | all services | ADR-002 (tokenized payment refs), ADR-003 | L1/UC4 (code review for secrets/PII) |
| BR-08 Independent, low-risk releases | TR-10, TR-13 | all services | ADR-001 (microservices), contract-first APIs | L1/UC2 (OpenAPI contracts) |

## Coverage Check

- Every business requirement (BR-01…BR-08) maps to at least one technical requirement. ✅
- Every technical requirement maps to an owning service defined in `architecture/architecture.json`. ✅
- Every architecture-level decision referenced here has a corresponding ADR in `adr/`. ✅
- No requirement currently traces to AI-autonomous payment decision logic — intentionally, per ADR-003. ✅
