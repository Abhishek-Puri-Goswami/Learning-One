# Technical Requirements — Online Shopping System

AI-assisted conversion of business requirements into technical requirements, produced using the "Good Prompt" pattern from `prompt-engineering/prompts-used.md` (context + constraints + structured output), then reviewed by a human engineer.

| ID | Technical Requirement | Derived From | Owning Service |
|---|---|---|---|
| TR-01 | Expose `GET /api/v1/products`, `GET /api/v1/products/{id}`, `GET /api/v1/products/search` with pagination and full-text search support. | BR-01 | product-catalog-service |
| TR-02 | Maintain a per-user/session cart in a low-latency store (Redis) with add/update/remove endpoints. | BR-02 | cart-service |
| TR-03 | Revalidate price and stock from the Catalog service at checkout time rather than trusting cached cart data. | BR-02, BR-07 | cart-service, order-management-service |
| TR-04 | Create an order from a validated cart via `POST /api/v1/orders`, using an idempotency key supplied by the client. | BR-03 | order-management-service |
| TR-05 | Expose order status and history via `GET /api/v1/orders/{id}` and `GET /api/v1/users/{userId}/orders`. | BR-03 | order-management-service |
| TR-06 | Authorize and capture payments through a licensed payment gateway; store only tokenized references, never raw card data. | BR-04, BR-07 | payment-service |
| TR-07 | Support refunds via `POST /api/v1/payments/{id}/refund`, traceable to the original transaction. | BR-04 | payment-service |
| TR-08 | Support registration, JWT-based login, and profile/address management. | BR-05 | user-management-service |
| TR-09 | Validate JWTs at the API Gateway and again at each downstream service (defense in depth). | BR-05, BR-07 | api-gateway, all services |
| TR-10 | Architect the system as independently deployable microservices so that each functional area can scale and release independently. | BR-06, BR-08 | all services (see ADR-001) |
| TR-11 | Use a Saga (event choreography) between Order, Catalog (stock reservation), and Payment instead of a distributed transaction. | BR-06, BR-08 | order-management-service |
| TR-12 | Mask/redact PII in all logs across services; no card data or plaintext credentials in logs. | BR-07 | all services |
| TR-13 | Design every service contract-first with an OpenAPI 3.0 spec before implementation. | (project constraint) | all services (see L1/UC2) |
| TR-14 | System must sustain 10,000 concurrent users; catalog reads must be cacheable to avoid database overload during peak traffic. | BR-06 | product-catalog-service, api-gateway |
