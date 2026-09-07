# Vector Database Schema — Design Notes

Deliverable: "Vector database schema" (companion to `vector-database-schema.sql`).

## Choice: PostgreSQL + pgvector

Evaluated against L2's reference guide's own "Key Factors for Choosing a Vector Database" checklist:

| Factor | This corpus | Choice |
|---|---|---|
| Dataset size | 7 documents / 37 chunks today, expected to grow to the full policy manual + future documents — still well under 1M vectors | pgvector comfortably handles this; no need for Milvus/Pinecone-scale infrastructure yet |
| Deployment preference | Secure Bank already runs PostgreSQL for `user-management-service`/`order-service` (L1) | Reuse existing operational expertise rather than introducing a new datastore |
| Performance & latency | HNSW index target: <200ms retrieval | pgvector's HNSW index (available since pgvector 0.5.0) meets this at this scale |
| Metadata & filtering | Need to filter by `document_category` (loan/FD/KYC/etc.) | Native SQL `WHERE document_category = ...` alongside vector search — no extra system needed |
| Security & compliance | Banking data; RBI/audit requirements per `Secure_Bank_policy_Manual_input-docs.pdf` §7 (Information Security) | PostgreSQL has mature encryption-at-rest, RBAC via roles, and audit logging (see `retrieval_audit_log` table) |
| Cost | Foundation/early-stage use case | Open-source, self-hosted — no new vendor contract needed to validate the approach |

If/when the corpus grows past ~1-5M vectors or cross-region low-latency replication becomes a hard requirement, the L2 reference guide's recommended path (Pinecone / Weaviate Cloud / Azure AI Search, all with first-class Java SDKs) is documented as the scale-up path — the `VectorStore` interface in `rag-core` was written so the swap is an implementation change, not an API change.

## Field-Level Notes

- **`embedding_model_id`**: added specifically to prevent the #1 failure mode called out in the L2 reference guide (§3.1): "you must use the SAME embedding model for indexing and querying." Storing the model id alongside every vector lets a query-time check reject (or explicitly re-embed) any mismatch instead of silently computing meaningless similarity scores against a different model's vector space.
- **`access_policy`**: mirrors the `access_policy: "read-only, redact-PII"` pattern already used in the AI Assistant Use Cases showcase (L1's reference PDF) and the L2 HLD's guardrail requirements.
- **`retrieval_audit_log.below_threshold`**: directly supports the hallucination-guardrail behavior demonstrated in `rag-core`'s `Main.java` and analyzed in `reports/hallucination-risk-analysis.md` — every query where the system correctly (or incorrectly) said "I don't know" is queryable for compliance review.

## What `rag-core`'s In-Memory `VectorStore` Does Differently (and why that's OK for this use case)

`VectorStore.java` in `rag-core/` is a plain `List`-backed in-memory index with linear-scan cosine similarity — it does **not** use pgvector or an HNSW index, because this use case's deliverable is "establish a strong foundation" with something that actually runs and is testable in this sandbox (see README's Maven Central limitation note for why a real Postgres/pgvector integration test isn't included here). At 37 chunks, linear scan is instant; this is explicitly a local/dev stand-in for the schema above, not a claim that linear scan is production-appropriate at scale.
