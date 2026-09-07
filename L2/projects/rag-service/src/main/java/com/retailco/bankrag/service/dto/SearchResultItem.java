package com.retailco.bankrag.service.dto;

public record SearchResultItem(
        String chunkId,
        String sourceDocument,
        int chunkIndex,
        String text,
        double score
) {
}
