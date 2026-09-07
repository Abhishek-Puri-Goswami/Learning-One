package com.retailco.bankrag.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines two different ways of finding relevant text into one better
 * score. Semantic search (see {@code VectorStore}) understands MEANING —
 * it can match a paraphrased question — but might miss an exact, rare
 * term. Keyword search ({@code KeywordSearcher}) is the opposite: exact
 * and predictable, but blind to synonyms. Blending both covers each one's
 * weak spot.
 * <p>
 * Here's how {@code search()} below works, step by step:
 * <ol>
 *   <li>Run semantic search AND keyword search separately, over every
 *       chunk (not just the top few), so nothing gets ruled out too
 *       early.</li>
 *   <li>Put each method's scores into a lookup table, keyed by chunk id.</li>
 *   <li>For every chunk that showed up in EITHER search, combine its two
 *       scores into one: {@code semanticWeight * semanticScore +
 *       keywordWeight * keywordScore} (if a chunk only appeared in one
 *       search, its missing score just counts as 0).</li>
 *   <li>Sort everything by that combined score and keep only the best
 *       few.</li>
 * </ol>
 * The weights (0.6 for semantic, 0.4 for keyword) are just a reasonable
 * starting point, not a fixed rule — whoever creates this class passes in
 * its own weights through the constructor, so it can be reused with
 * different balances of the two search types.
 */
public class HybridSearcher {

    private final VectorStore vectorStore;
    private final KeywordSearcher keywordSearcher;
    private final double semanticWeight;
    private final double keywordWeight;

    public HybridSearcher(VectorStore vectorStore, KeywordSearcher keywordSearcher,
                           double semanticWeight, double keywordWeight) {
        this.vectorStore = vectorStore;
        this.keywordSearcher = keywordSearcher;
        this.semanticWeight = semanticWeight;
        this.keywordWeight = keywordWeight;
    }

    public List<ScoredChunk> search(String query, int topK, double semanticThreshold) {
        List<ScoredChunk> semanticResults = vectorStore.semanticSearch(query, vectorStore.size(), semanticThreshold);
        List<ScoredChunk> keywordResults = keywordSearcher.search(vectorStore.allChunks(), query, vectorStore.size());

        Map<String, Double> semanticScores = new HashMap<>();
        for (ScoredChunk sc : semanticResults) {
            semanticScores.put(sc.chunk().id(), sc.score());
        }
        Map<String, Double> keywordScores = new HashMap<>();
        for (ScoredChunk sc : keywordResults) {
            keywordScores.put(sc.chunk().id(), sc.score());
        }
        Map<String, Chunk> byId = new HashMap<>();
        for (Chunk c : vectorStore.allChunks()) {
            byId.put(c.id(), c);
        }

        Map<String, Double> combined = new HashMap<>();
        for (String id : union(semanticScores.keySet(), keywordScores.keySet())) {
            double blended = semanticWeight * semanticScores.getOrDefault(id, 0.0)
                    + keywordWeight * keywordScores.getOrDefault(id, 0.0);
            combined.put(id, blended);
        }

        return combined.entrySet().stream()
                .map(e -> new ScoredChunk(byId.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private java.util.Set<String> union(java.util.Set<String> a, java.util.Set<String> b) {
        java.util.Set<String> result = new java.util.HashSet<>(a);
        result.addAll(b);
        return result;
    }
}
