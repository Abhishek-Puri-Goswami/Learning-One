package com.retailco.bankrag.core;

import java.util.HashMap;
import java.util.Map;

// CONCEPT: Strategy pattern implementation -- the "offline fallback"
// EmbeddingModel, using the classic ML "feature hashing" (hashing trick).
// PURPOSE: Lets the whole RAG pipeline run with zero external dependencies
// and zero network calls, so it's always demonstrable even without an
// OpenAI API key. This is the automatic fallback whenever
// OpenAiEmbeddingModel.isConfigured() is false (see each module's
// *Config.java).
//
// HOW IT WORKS (step by step, see embed() below):
// 1. Tokenize the text into lowercase words.
// 2. Hash each word to a bucket index in [0, dimensions) using its
//    hashCode() (Math.floorMod handles negative hash codes).
// 3. Count how many times each bucket is hit -- this builds a
//    bag-of-words vector where "similar word overlap" -> "similar vector."
// 4. L2-normalize the vector (divide by its length) so cosine similarity
//    between two vectors only measures direction/overlap, not raw length.
//
// WHY THIS APPROACH: it needs no trained model, no external service, and
// no big vocabulary table -- just a hash function -- so it's genuinely
// runnable anywhere Java runs. The trade-off (IMPORTANT): it only captures
// literal word overlap, not real semantic meaning -- "car" and "automobile"
// get completely unrelated vectors here, whereas a real embedding model
// (OpenAiEmbeddingModel) would place them close together. This is a
// disclosed, deliberate stand-in, not a production-quality embedding model.
public class LocalHashingEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public LocalHashingEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public double[] embed(String text) {
        double[] vector = new double[dimensions];
        String[] tokens = Chunker.tokenize(text.toLowerCase());

        Map<Integer, Integer> termCounts = new HashMap<>();
        for (String token : tokens) {
            String cleaned = token.replaceAll("[^a-z0-9]", "");
            if (cleaned.isBlank()) continue;
            int bucket = Math.floorMod(cleaned.hashCode(), dimensions);
            termCounts.merge(bucket, 1, Integer::sum);
        }

        for (Map.Entry<Integer, Integer> entry : termCounts.entrySet()) {
            vector[entry.getKey()] = entry.getValue();
        }

        return l2Normalize(vector);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private double[] l2Normalize(double[] vector) {
        double sumSquares = 0;
        for (double v : vector) {
            sumSquares += v * v;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm == 0) {
            return vector;
        }
        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }
}
