package com.retailco.bankrag.service.dto;

/**
 * The JSON shape we return after a successful ingest — built by
 * {@code IngestionService} and turned into JSON automatically by Spring.
 */
public record IngestResponse(
        int documentsLoaded,
        int chunksIndexed,
        int chunkSizeTokens,
        int overlapTokens
) {
}
