package com.retailco.bankrag.service.dto;

/**
 * The API's own shape for one search result, deliberately kept separate
 * from the internal {@code ScoredChunk} type (see {@code SearchService},
 * which converts between the two). Keeping them separate means our
 * internal classes can change freely without breaking the public API,
 * and vice versa.
 */
public record SearchResultItem(
        String chunkId,
        String sourceDocument,
        int chunkIndex,
        String text,
        double score
) {
}
