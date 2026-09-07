package com.retailco.bankrag.service.dto;

public record IngestResponse(
        int documentsLoaded,
        int chunksIndexed,
        int chunkSizeTokens,
        int overlapTokens
) {
}
