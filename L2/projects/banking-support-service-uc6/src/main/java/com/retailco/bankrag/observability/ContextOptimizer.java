package com.retailco.bankrag.observability;

import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ScoredChunk;

import java.util.ArrayList;
import java.util.List;

// CONCEPT: Token-budget algorithm (a greedy "knapsack-lite" trimming
// strategy) for controlling LLM prompt cost/size.
// PURPOSE: RagAssistant's PromptTemplate normally sends every retrieved
// chunk's full text to the LLM. That's fine for a small topK, but doesn't
// scale to a larger retrieval count, longer documents, or a
// cost-sensitive deployment. This class enforces a hard token BUDGET
// across the retrieved chunks before they reach the prompt.
//
// HOW IT WORKS (see optimize() below, step by step):
// 1. Walk the chunks in their EXISTING order (already sorted by relevance
//    score, descending -- assumed, not re-sorted, since the caller
//    already did that work).
// 2. For each chunk, check if adding its token count would still fit
//    under maxContextTokens.
// 3. If it fits, keep it and add to the running total; if not, drop the
//    WHOLE chunk (never truncate mid-chunk).
//
// WHY drop whole chunks rather than truncate text: a half-sentence from a
// policy document (e.g. cut off mid-clause) could be more misleading than
// no information at all -- an incomplete rule can look like a complete one.
// WHY highest-scoring chunks are processed first: under a tight budget,
// this guarantees the MOST relevant chunks are the ones kept, and only the
// least relevant ones get dropped.
public final class ContextOptimizer {

    public record OptimizationResult(List<ScoredChunk> keptChunks, int keptTokens,
                                      int droppedChunkCount, int droppedTokens) {
    }

    private final int maxContextTokens;

    public ContextOptimizer(int maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
    }

    public OptimizationResult optimize(List<ScoredChunk> retrievedChunks) {
        // retrievedChunks is already score-sorted descending by
        // HybridSearcher/VectorStore -- kept here as an explicit assumption
        // rather than re-sorting, since re-sorting a list that's already in
        // the right order would just be wasted work every call.
        List<ScoredChunk> kept = new ArrayList<>();
        int usedTokens = 0;
        int droppedCount = 0;
        int droppedTokens = 0;

        for (ScoredChunk sc : retrievedChunks) {
            int chunkTokens = Chunker.tokenize(sc.chunk().text()).length;
            if (usedTokens + chunkTokens <= maxContextTokens) {
                kept.add(sc);
                usedTokens += chunkTokens;
            } else {
                droppedCount++;
                droppedTokens += chunkTokens;
            }
        }

        return new OptimizationResult(kept, usedTokens, droppedCount, droppedTokens);
    }
}
