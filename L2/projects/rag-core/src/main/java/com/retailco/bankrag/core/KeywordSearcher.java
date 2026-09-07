package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure keyword (lexical) search: scores each chunk by the fraction of query
 * terms it literally contains, case-insensitively. Used as the baseline in
 * reports/retrieval-comparison-summary.md's "keyword vs semantic vs hybrid"
 * comparison, per L2 HLD UseCase1's required functional scope.
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
