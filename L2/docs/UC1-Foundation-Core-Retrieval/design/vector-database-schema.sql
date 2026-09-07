-- L2 UC1 deliverable: "Vector database schema."
-- Target: PostgreSQL + pgvector (chosen over a managed vector DB for this
-- foundation use case since Secure Bank's stack already standardizes on
-- PostgreSQL for other services -- see L2's reference guide section 3.4,
-- "Choosing a Vector Database": pgvector / Azure AI Search recommended for
-- banking due to encryption, RBAC, and audit-log requirements).
--
-- The in-memory VectorStore.java in rag-core/ is the local/dev equivalent
-- of this schema (same conceptual fields: id, source_document, chunk_index,
-- chunk_text, embedding, created_at) -- swap VectorStore's implementation
-- for one backed by this table in production, keeping the same
-- EmbeddingModel-first contract described in embedding-generation-module.md.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE policy_chunks (
    id                  TEXT PRIMARY KEY,             -- e.g. 'loan_processing_policy.txt#2'
    source_document     TEXT NOT NULL,                 -- original file name / document id
    chunk_index         INTEGER NOT NULL,               -- position within the source document
    chunk_text          TEXT NOT NULL,
    embedding           VECTOR(1536) NOT NULL,          -- dimension matches the production embedding
                                                          -- model in use (e.g. 1536 for
                                                          -- text-embedding-3-small); MUST match
                                                          -- exactly across indexing and querying
                                                          -- (see embedding-generation-module.md)
    embedding_model_id  TEXT NOT NULL,                  -- e.g. 'text-embedding-3-small-v1' --
                                                          -- guards against silently mixing vectors
                                                          -- from two different embedding models
    document_category   TEXT,                           -- e.g. 'loan_policy', 'fd_policy',
                                                          -- 'kyc_policy' -- supports metadata
                                                          -- filtering (L2 reference guide 3.4 #4)
    access_policy       TEXT NOT NULL DEFAULT 'read-only, redact-PII',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ANN index for fast similarity search (L2 reference guide 3.4 #3:
-- "Should support ANN search (HNSW/IVF)"). HNSW chosen over IVFFlat for
-- better recall at query time on a corpus this size.
CREATE INDEX policy_chunks_embedding_hnsw_idx
    ON policy_chunks
    USING hnsw (embedding vector_cosine_ops);

-- Metadata filter index (category-scoped retrieval, e.g. "only search loan policy chunks").
CREATE INDEX policy_chunks_category_idx ON policy_chunks (document_category);

-- Audit log table (L2 reference guide section 6.4, "Audit Logging" --
-- required for a banking RAG system): every retrieval is logged with the
-- query, which chunks were returned, and the similarity scores, so a
-- compliance reviewer can reconstruct why the system answered the way it did.
CREATE TABLE retrieval_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    query_text      TEXT NOT NULL,
    requested_by    TEXT,                    -- user/session id, PII-masked if applicable
    returned_chunk_ids TEXT[] NOT NULL,
    similarity_scores  DOUBLE PRECISION[] NOT NULL,
    below_threshold    BOOLEAN NOT NULL,      -- true if the guardrail fired ("I don't know")
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX retrieval_audit_log_created_at_idx ON retrieval_audit_log (created_at);
