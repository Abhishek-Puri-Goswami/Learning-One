# Architecture Diagrams

Deliverable: "Architecture documentation & diagrams," per L2 HLD UseCase6. Extends L2/UC5's diagrams (`L2/UC5-Final-Integrated-System/diagrams/architecture-diagrams.md`) with this use case's two additions: the RBAC decision inside `BankingToolService`, and the CI/CD pipeline that gates deployment.

## 1. Request flow, with RBAC + audit logging (sequence)

```mermaid
sequenceDiagram
    actor Customer
    participant FE as React Frontend (UC6)
    participant API as SupportController
    participant IC as IntentClassifier (UC3)
    participant ORA as ObservableRagAssistant (UC4)
    participant BTS as BankingToolService (UC3/UC6)
    participant JWT as JwtService (UC3)
    participant AP as AccessPolicy (UC6, RBAC)
    participant DB as BankingDataStore (UC3)
    participant AUD as StructuredAuditLogger (UC6)

    Customer->>FE: types a question
    FE->>API: POST /api/v1/support/ask [+ Authorization?]
    API->>IC: classify(query)
    alt POLICY_QUESTION
        IC-->>API: POLICY_QUESTION
        API->>ORA: ask(query)
        ORA-->>API: cached-or-fresh AssistantResponse
    else LIVE_DATA intent
        IC-->>API: ACCOUNT_BALANCE | TRANSACTION_HISTORY | LOAN_OUTSTANDING
        API->>BTS: getX(token, requestedCustomerId, ...)
        BTS->>JWT: verify(token)
        JWT-->>BTS: Valid claims | Invalid
        alt token invalid
            BTS->>AUD: log(DENIED_AUTHENTICATION)
            BTS-->>API: UnauthorizedException
        else token valid
            BTS->>AP: evaluate(subject, roles, requestedCustomerId)
            AP-->>BTS: Allowed(role, elevated) | Denied(reason)
            alt Denied
                BTS->>AUD: log(DENIED_AUTHORIZATION)
                BTS-->>API: UnauthorizedException
            else Allowed
                BTS->>AUD: log(ALLOWED_SELF | ALLOWED_ELEVATED)
                BTS->>DB: findByCustomerId(...)
                DB-->>BTS: raw records
                BTS-->>API: masked data (PiiMasking, unchanged from UC3)
            end
        end
    end
    API-->>FE: unified JSON response
    FE-->>Customer: rendered answer / table / denial
```

## 2. CI/CD pipeline (job graph)

```mermaid
graph LR
    subgraph "Perform lint checks"
        L1[lint: javac -Xlint]
        L2[frontend-build-and-lint: oxlint]
    end

    subgraph "Run tests / Validate JWT+RAG"
        T1[core-tests: SelfTests 14/14]
        T2[rbac-and-security-tests: RbacAndAuditSelfTests 14/14 + secret scan]
    end

    E[evaluation: run demo, validate trace-log.jsonl]
    FE[frontend-build-and-lint: vite build]
    MVN[maven-build-service: mvn clean verify]

    L1 --> T1
    L1 --> T2
    T1 --> E

    D[docker-build: build backend + frontend images]
    T1 --> D
    T2 --> D
    E --> D
    FE --> D
    MVN --> D

    DEPLOY["deploy (main branch only, all above must succeed)"]
    D --> DEPLOY

    style DEPLOY fill:#e8f4ea,stroke:#2e7d32
```

Every job in the left half of this graph (`lint`, `core-tests`, `rbac-and-security-tests`, `evaluation`, `frontend-build-and-lint`) runs commands that were **actually executed in this submission's sandbox** — see `banking-support-core/reports/` and `banking-support-frontend/reports/` for their real output. `maven-build-service` and `docker-build` need Maven Central / a Docker daemon this sandbox doesn't have; see `.github/workflows/ci-cd.yml`'s header comment and `cicd/README.md` for exactly what that means.
