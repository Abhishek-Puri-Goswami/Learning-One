package com.retailco.bankrag.service.dto;

// CONCEPT: Response DTO -- the JSON shape returned to the caller, built by
// IngestionService and serialized automatically by Spring/Jackson.
public record IngestResponse(
        int documentsLoaded,
        int chunksIndexed,
        int chunkSizeTokens,
        int overlapTokens
) {
}
