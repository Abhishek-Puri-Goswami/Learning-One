package com.retailco.bankrag.core;

import java.util.HashMap;
import java.util.Map;

/**
 * This is our OFFLINE, no-internet-needed stand-in for a real AI
 * embedding model. It automatically kicks in whenever no OpenAI API key
 * is configured (see each module's Config class), so the whole
 * application can still run and be demonstrated without any external
 * service or API key at all.
 * <p>
 * Here's how it fakes an "embedding" of some text, step by step (see
 * {@code embed()} below):
 * <ol>
 *   <li>Break the text into lowercase words.</li>
 *   <li>Turn each word into a number ("hash" it) that lands somewhere in
 *       a fixed-size range.</li>
 *   <li>Count how many times each of those "buckets" gets hit — this
 *       produces a vector where texts sharing more of the same words end
 *       up with more similar vectors.</li>
 *   <li>Scale the vector down to a consistent length, so comparing two
 *       vectors later only measures their DIRECTION (overlap), not how
 *       long the original text happened to be.</li>
 * </ol>
 * <p>
 * Why do it this way: it needs no trained AI model, no external service,
 * and no huge dictionary — just simple math — so it can genuinely run
 * anywhere Java runs. The trade-off: it only ever catches LITERAL word
 * overlap, not true meaning — "car" and "automobile" get completely
 * unrelated vectors here, whereas a real AI embedding model
 * ({@code OpenAiEmbeddingModel}) would recognize they mean similar
 * things and place them close together. This is a known, deliberate
 * limitation — not a claim that this is production-quality.
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
