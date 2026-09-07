# Online Shopping System — Logical Architecture Diagram

AI-generated logical architecture diagram derived from `architecture.json`. Rendered with Mermaid (viewable in GitHub, VS Code with the Mermaid extension, or any Mermaid-compatible viewer).

```mermaid
flowchart TB
    subgraph Client["Client Layer"]
        WEB["React Web App"]
    end

    GW["API Gateway<br/>(Spring Cloud Gateway)<br/>AuthN/AuthZ, Rate Limiting"]

    subgraph Services["Microservices"]
        USER["User Management Service<br/>PostgreSQL"]
        CATALOG["Product Catalog Service<br/>PostgreSQL + Elasticsearch"]
        CART["Cart Service<br/>Redis"]
        ORDER["Order Management Service<br/>PostgreSQL"]
        PAYMENT["Payment Service<br/>PostgreSQL (tokenized)"]
    end

    GATEWAY_PAY["External Payment Gateway<br/>(Stripe / Razorpay)"]
    BUS["Event Bus<br/>(Kafka / RabbitMQ)<br/>Saga Choreography"]

    WEB -->|HTTPS/REST| GW
    GW --> USER
    GW --> CATALOG
    GW --> CART
    GW --> ORDER
    GW --> PAYMENT

    CART -.->|revalidate price/stock| CATALOG
    ORDER -->|reserve stock event| BUS
    BUS -->|stock reserved / failed| ORDER
    ORDER -->|payment requested event| BUS
    BUS -->|payment result| ORDER
    PAYMENT <--> GATEWAY_PAY
    PAYMENT -->|publishes payment result| BUS

    classDef svc fill:#eef,stroke:#446,stroke-width:1px;
    class USER,CATALOG,CART,ORDER,PAYMENT svc;
```

## Notes

- **Contract-first**: every arrow between the Gateway and a service corresponds to an OpenAPI 3.0 contract (see `L1/UC2-Backend-API-Scaffolding`).
- **Saga, not 2PC**: Order Management orchestrates checkout via choreographed events on the bus rather than a distributed transaction, because Payment and Catalog are owned by independently deployable services (see `adr/ADR-001-microservices-vs-monolith.md`).
- **Cart is not authoritative**: price and stock shown in the cart are always revalidated against the Catalog service at checkout time to avoid stale-data risk.
- This diagram is the "AI-generated architecture diagram (logical)" deliverable required by USE CASE 1.
