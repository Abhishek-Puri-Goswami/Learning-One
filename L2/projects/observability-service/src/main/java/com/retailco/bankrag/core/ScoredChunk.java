package com.retailco.bankrag.core;

// CONCEPT: Value object pairing a Chunk with a relevance score.
// PURPOSE: The common return type for every search strategy in this
// package (KeywordSearcher, VectorStore.semanticSearch, HybridSearcher) so
// callers can sort/compare/display results the same way regardless of
// which search method produced them.
public record ScoredChunk(Chunk chunk, double score) {
}
