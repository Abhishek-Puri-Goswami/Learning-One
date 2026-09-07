package com.retailco.bankrag.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid search: weighted combination of semantic (vector) score and
 * keyword score, re-ranking the union of both result sets. Weighting
 * defaults (0.6 semantic / 0.4 keyword) are the recommended starting point
 * per L2's reference guide's "hybrid search" mention under vector DB
 * selection factors -- tunable per design/chunking-configuration.md.
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
