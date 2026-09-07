package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// CONCEPT: Lexical (keyword) search -- the classic, non-ML baseline for
// information retrieval.
// PURPOSE: Scores a chunk by what fraction of the query's distinct words
// it literally contains (case-insensitive). Doesn't understand meaning at
// all -- "car" and "automobile" share zero overlap here -- but it's exact
// and predictable, which makes it a useful baseline to compare semantic
// search against (see HybridSearcher, which blends both).
// WHY IT MATTERS: a good RAG system usually needs semantic search for
// paraphrased questions AND keyword search for exact terms (account
// numbers, policy codes, specific named products) that an embedding model
// might blur together with similar-sounding text.
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
