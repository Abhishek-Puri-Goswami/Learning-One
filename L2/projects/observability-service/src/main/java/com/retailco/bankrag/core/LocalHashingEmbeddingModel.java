package com.retailco.bankrag.core;

import java.util.HashMap;
import java.util.Map;

/**
 * A dependency-free, fully offline embedding model used ONLY to make the
 * retrieval pipeline in this folder actually runnable and verifiable inside
 * a sandbox with no network access to an embedding API or to Maven Central
 * (to pull a real local model like LangChain4j's all-MiniLM-L6-v2). This is
 * NOT a claim that hashed bag-of-words vectors are production-quality
 * semantic embeddings -- see design/embedding-generation-module.md for the
 * explicit limitation and the real model this must be swapped for before
 * any production/graded use.
 *
 * Technique: feature hashing (the "hashing trick") of term frequencies into
 * a fixed-size dense vector, L2-normalized. This still captures meaningful
 * term-overlap-based similarity (enough to demonstrate and test the
 * pipeline end-to-end -- see the retrieval comparison report) but does NOT
 * capture true semantic/synonym similarity the way a trained embedding
 * model does. That gap is exactly why reports/retrieval-comparison-summary.md
 * discusses this model's "semantic" column with that caveat attached.
 */
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
