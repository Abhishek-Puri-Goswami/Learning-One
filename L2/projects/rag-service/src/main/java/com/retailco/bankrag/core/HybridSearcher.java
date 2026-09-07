package com.retailco.bankrag.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CONCEPT: Hybrid search -- combines two different ranking signals
// (semantic/vector similarity and lexical/keyword overlap) into one score.
// PURPOSE: Semantic search (VectorStore) is good at "meaning" but can miss
// an exact rare term; keyword search (KeywordSearcher) is good at exact
// terms but blind to synonyms/paraphrasing. Blending both compensates for
// each one's weak spot.
//
// HOW IT WORKS (see search() below):
// 1. Run semantic search AND keyword search independently, over ALL
//    chunks (not just topK) so no candidate is prematurely excluded.
// 2. Put each result set's scores in a map keyed by chunk id.
// 3. For every chunk id that appears in EITHER result set (the `union`),
//    compute a blended score: semanticWeight * semanticScore +
//    keywordWeight * keywordScore (missing scores default to 0.0).
// 4. Sort by blended score, descending, and keep only the top K.
//
// WHY these particular weights (0.6 semantic / 0.4 keyword): a tunable
// starting point, not a hardcoded law -- callers pass their own weights in
// the constructor, so this class stays reusable.
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
