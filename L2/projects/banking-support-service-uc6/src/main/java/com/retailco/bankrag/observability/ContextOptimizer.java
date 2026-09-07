package com.retailco.bankrag.observability;

import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ScoredChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Normally, every retrieved chunk's full text gets sent to the AI model.
 * That's fine when only a few chunks are retrieved, but it doesn't scale
 * well to more chunks, longer documents, or a cost-sensitive deployment.
 * This class enforces a hard limit on how many "tokens" (roughly, words)
 * of context we send, trimming the list down before it reaches the
 * prompt.
 * <p>
 * Here's how {@code optimize()} works, step by step:
 * <ol>
 *   <li>Walk through the chunks in the order they were given — they're
 *       already sorted with the most relevant first.</li>
 *   <li>For each chunk, check whether adding it would still fit under
 *       the token budget.</li>
 *   <li>If it fits, keep it. If not, drop the WHOLE chunk — never cut a
 *       chunk in half.</li>
 * </ol>
 * <p>
 * Why drop whole chunks instead of trimming the text: half a sentence
 * from a policy document — cut off mid-thought — could be more misleading
 * than having no information there at all. An incomplete rule can look
 * exactly like a complete one.
 * <p>
 * Why the most relevant chunks are checked first: under a tight budget,
 * this guarantees the MOST useful chunks are the ones that make it in,
 * and only the least useful ones get dropped when space runs out.
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
        // We assume retrievedChunks arrives already sorted, most relevant
        // first (the search classes upstream already do that sorting) —
        // re-sorting an already-sorted list here would just be wasted work.
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
