package com.retailco.bankrag.service.dto;

// CONCEPT: Response DTO -- the API's own shape for one search result,
// deliberately separate from core's `ScoredChunk` (see SearchService,
// which maps ScoredChunk -> SearchResultItem). Keeping them separate
// means the internal `core` package's types can change without breaking
// the public REST API contract, and vice versa.
public record SearchResultItem(
        String chunkId,
        String sourceDocument,
        int chunkIndex,
        String text,
        double score
) {
}
