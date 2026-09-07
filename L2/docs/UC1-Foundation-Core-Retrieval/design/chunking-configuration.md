# Chunking Configuration Design — L2 UC1

Deliverable: "Chunking configuration design."

## Chosen Configuration

```java
ChunkingConfig.defaultConfig() == new ChunkingConfig(chunkSizeTokens = 180, overlapTokens = 40)
```

Implemented in `rag-core/src/main/java/.../ChunkingConfig.java` and `Chunker.java` (sliding-window, whitespace-token approximation — see the limitation note below).

## Rationale

L2's reference guide (`Building RAG Systems-Initial-Guide.pdf`, section 2 and section 3.3) recommends **300-1200 tokens with 100-150 overlap** as a general-purpose range, and **300-1200 tokens / 100-150 overlap** again in the companion `Initial-RAG-complete Reference_.docx`. Both are aimed at a general document corpus. For this specific corpus — Secure Bank's Policy & Operations Manual — a smaller chunk size was chosen deliberately:

- **Policy manual structure is already dense and section-scoped.** Each numbered subsection (e.g., "4.2 Eligibility Assessment," "11.3 Penalties & Violation Fees") is a self-contained rule with its own numeric thresholds. A 500+ token chunk risks blending two unrelated sub-rules (e.g., Home Loan LTV norms bleeding into Personal Loan eligibility) into one retrieved chunk, which increases the chance an LLM cites the wrong rule.
- **Policy answers are usually short, specific facts** (an interest rate band, an income threshold, an escalation timeline) rather than long narrative passages, so a smaller, more precise chunk improves retrieval precision at the cost of slightly more chunks to store.
- **180 tokens** keeps most chunks within a single policy sub-point (verified empirically against this corpus — see `reports/retrieval-comparison-summary.md`, where `loan_processing_policy.txt` splits into 5 chunks that align reasonably well with its `4.1`/`4.2`/fee-schedule subsections).
- **40-token overlap** (~22% of chunk size) is below the reference guide's 100-150 absolute range but proportionally similar (guide's 100/500 = 20%), scaled down consistently for the smaller chunk size, to avoid a rule's opening clause being cut off between chunks.

## Known Limitation

Token counts here are computed by whitespace-splitting (see `Chunker.tokenize`), not a real BPE/subword tokenizer (e.g., `tiktoken` for OpenAI models). This is an approximation — actual LLM token counts for the same text will differ (typically similar order of magnitude for English prose, but not exact). No LLM-vendor tokenizer library was reachable in this sandbox (see README's Maven Central limitation). **Before production use**, swap `Chunker.tokenize` for the real tokenizer of whatever LLM provider is chosen, and re-validate chunk sizes.

## Configuration Is a Constructor Parameter, Not a Hardcoded Constant

`ChunkingConfig` is a validated, immutable record (rejects `overlapTokens >= chunkSizeTokens` at construction) so it can be tuned per-document-type without code changes — e.g., a future use case ingesting long-form regulatory circulars might reasonably want a larger `chunkSizeTokens`.
