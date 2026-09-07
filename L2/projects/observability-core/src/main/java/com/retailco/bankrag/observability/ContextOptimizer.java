package com.retailco.bankrag.observability;

import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ScoredChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Deliverable: "Context optimization." L2 HLD UseCase4 System
 * Responsibilities: "Optimize context size."
 *
 * L2/UC2's PromptTemplate sends every retrieved chunk's full text to the
 * LLM unconditionally. That's fine at topK=3 with ~180-token chunks (the
 * corpus's real numbers, see L2/UC1's design/chunking-configuration.md),
 * but doesn't scale: a larger topK, longer documents, or a
 * cost-per-token-sensitive deployment needs a hard cap. This class enforces
 * a token BUDGET across the retrieved set, keeping the highest-scoring
 * chunks first and dropping (not truncating mid-chunk -- a half-chunk is
 * worse than no chunk, since a policy sentence cut off mid-clause could
 * itself become a source of an incomplete/misleading answer) whatever
 * doesn't fit.
 */
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
