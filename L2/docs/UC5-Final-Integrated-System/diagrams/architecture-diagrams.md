# Architecture Diagrams

Deliverable: "Architecture diagrams," per L2 HLD UseCase5. Two diagrams: the end-to-end request flow through the final integrated system, and the component/package map showing which L2 use case each piece came from.

## 1. Request flow (sequence)

```mermaid
sequenceDiagram
    actor Customer
    participant API as SupportController<br/>(POST /api/v1/support/ask)
    participant IC as IntentClassifier (UC3)
    participant ORA as ObservableRagAssistant (UC4)
    participant Cache as QueryCache (UC4)
    participant RA as RagAssistant (UC2)
    participant Guard as Guardrails (UC2)
    participant VS as VectorStore (UC1)
    participant LLM as ExtractiveStubLlmClient (UC2)
    participant BTS as BankingToolService (UC3)
    participant JWT as JwtService (UC3)
    participant DB as BankingDataStore (UC3)

    Customer->>API: query [+ Authorization: Bearer token]
    API->>IC: classify(query)
    alt POLICY_QUESTION
        IC-->>API: POLICY_QUESTION
        API->>ORA: ask(query)
        ORA->>Cache: get(query)
        alt cache hit
            Cache-->>ORA: cached AssistantResponse
        else cache miss
            ORA->>RA: ask(query)
            RA->>Guard: injection / unsafe-advice check
            alt blocked
                Guard-->>RA: blocked
            else allowed
                RA->>VS: hybridSearch(query, topK=3)
                VS-->>RA: scored chunks
                alt weak retrieval (margin/threshold guardrail)
                    RA-->>ORA: fallback response
                else strong retrieval
                    RA->>LLM: generate(prompt)
                    LLM-->>RA: cited answer
                    RA-->>ORA: AssistantResponse (evaluated + traced)
                end
            end
            ORA->>Cache: put(query, response)
        end
        ORA-->>API: ObservedResponse
    else LIVE_DATA intent (balance / transactions / loan)
        IC-->>API: ACCOUNT_BALANCE | TRANSACTION_HISTORY | LOAN_OUTSTANDING
        API->>BTS: getX(token, customerId, ...)
        BTS->>JWT: verify(token)
        JWT-->>BTS: Valid claims | Invalid
        alt invalid or wrong customer
            BTS-->>API: UnauthorizedException
        else authorized
            BTS->>DB: findByCustomerId(...)
            DB-->>BTS: raw records
            BTS-->>API: masked data
        end
    else AMBIGUOUS
        IC-->>API: AMBIGUOUS
        API-->>Customer: clarification request
    end
    API-->>Customer: unified JSON response
```

## 2. Component map (which use case each piece came from)

```mermaid
graph TD
    subgraph UC1["L2/UC1 — Foundation & Core Retrieval"]
        Chunker[Chunker / ChunkingConfig]
        Embed[LocalHashingEmbeddingModel]
        VS[VectorStore]
        Hybrid[HybridSearcher / KeywordSearcher]
    end

    subgraph UC2["L2/UC2 — End-to-End RAG Banking Assistant"]
        Prompt[PromptTemplate]
        Guards[PromptInjectionGuard / UnsafeQueryGuard]
        LLM[ExtractiveStubLlmClient]
        Cite[CitationExtractor]
        Trace[TraceLogger]
        Eval[EvaluationHarness]
        RA[RagAssistant]
    end

    subgraph UC3["L2/UC3 — Secure Banking Data Integration"]
        JWT[JwtService]
        Mask[PiiMasking]
        Store[BankingDataStore]
        BTS[BankingToolService]
        IC[IntentClassifier]
    end

    subgraph UC4["L2/UC4 — Intelligence Maturity & Optimization"]
        Cache[QueryCache]
        Metrics[MetricsRecorder]
        Cost[CostEstimator]
        CtxOpt[ContextOptimizer]
        ORA[ObservableRagAssistant]
    end

    subgraph UC5["L2/UC5 — Final Integrated System (this use case)"]
        IBA[IntegratedBankingAssistant]
        API[SupportController]
    end

    Chunker --> VS
    Embed --> VS
    VS --> Hybrid
    Hybrid --> RA
    Prompt --> RA
    Guards --> RA
    LLM --> RA
    Cite --> RA
    Trace --> RA
    Eval --> RA

    RA --> ORA
    Cache --> ORA
    Metrics --> ORA
    Cost --> ORA

    JWT --> BTS
    Mask --> BTS
    Store --> BTS

    IC --> IBA
    ORA --> IBA
    BTS --> IBA
    IBA --> API

    style UC5 fill:#e8f4ea,stroke:#2e7d32
```

Both diagrams describe `banking-support-core`'s `Main.java`/`SelfTests.java`, which actually exercises every path shown above — see `reports/integrated-demo-run-log.txt` for the real, corresponding output.
