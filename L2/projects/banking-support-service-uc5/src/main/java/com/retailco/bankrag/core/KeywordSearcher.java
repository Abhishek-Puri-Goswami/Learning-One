package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The classic, simple way to search text: score each chunk by what
 * fraction of the question's words it literally contains
 * (case-insensitive). This has zero understanding of MEANING — "car" and
 * "automobile" share no overlap here at all — but it's exact and
 * predictable, which makes it useful to combine with semantic search
 * (see {@code HybridSearcher}).
 * <p>
 * Why this still matters even with AI-powered search available: a good
 * search system usually needs BOTH — semantic search for paraphrased
 * questions, and keyword search for exact terms like account numbers,
 * policy codes, or specific product names — the kind of exact strings an
 * AI embedding model might blur together with similar-sounding text.
 */
public class KeywordSearcher {

    public List<ScoredChunk> search(List<Chunk> chunks, String query, int topK) {
        Set<String> queryTerms = toTermSet(query);
        List<ScoredChunk> scored = new ArrayList<>();

        for (Chunk chunk : chunks) {
            Set<String> chunkTerms = toTermSet(chunk.text());
            long matches = queryTerms.stream().filter(chunkTerms::contains).count();
            double score = queryTerms.isEmpty() ? 0 : (double) matches / queryTerms.size();
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return scored.size() > topK ? scored.subList(0, topK) : scored;
    }

    private Set<String> toTermSet(String text) {
        // HashSet, not Set.of(...) -- Set.of throws IllegalArgumentException
        // on duplicate elements, and tokenized policy text has plenty of
        // repeated words (the, and, of, ...).
        return new HashSet<>(Arrays.asList(Chunker.tokenize(text.toLowerCase())));
    }
}
