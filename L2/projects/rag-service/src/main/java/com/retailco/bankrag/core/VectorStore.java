package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A simple, in-memory stand-in for a real vector database (like
 * pgvector, Pinecone, or Weaviate). It stores every chunk alongside its
 * embedding vector, and answers the key question of retrieval: "which
 * stored chunks are most similar to this question?"
 * <p>
 * The flow: {@code Chunker} produces Chunks → {@code index(chunk)}
 * embeds and stores each one → {@code semanticSearch(query, ...)} embeds
 * the incoming question the exact same way and ranks every stored chunk
 * by similarity → the best matches get sent to the AI model as context
 * for answering (see {@code RagAssistant} in the assistant package).
 * <p>
 * Why in-memory: two lists that stay in sync ({@code chunks} and
 * {@code embeddings}, where the same position in each list refers to the
 * same chunk) keep this class dependency-free, so the whole thing runs
 * without needing a real database. A production system could swap this
 * class's internals for a real vector database without anything else in
 * the app needing to change — that's the benefit of keeping storage
 * details hidden behind one class.
 * <p>
 * One important rule: comparing vectors only makes sense if the query and
 * the stored chunks were embedded using the exact same model — see
 * {@code EmbeddingModel} for why.
 */
public class VectorStore {

    private final EmbeddingModel embeddingModel;
    private final List<Chunk> chunks = new ArrayList<>();
    private final List<double[]> embeddings = new ArrayList<>();

    public VectorStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public void index(Chunk chunk) {
        chunks.add(chunk);
        embeddings.add(embeddingModel.embed(chunk.text()));
    }

    public int size() {
        return chunks.size();
    }

    /**
     * Semantic search: embeds the query with the SAME embedding model used
     * for indexing (per L2's reference guide 3.1 rule), then ranks all
     * chunks by cosine similarity, applying a similarity threshold to filter
     * weak matches (see L2 HLD UseCase1 "Set similarity thresholds to filter
     * weak matches").
     */
    public List<ScoredChunk> semanticSearch(String query, int topK, double similarityThreshold) {
        double[] queryVector = embeddingModel.embed(query);
        List<ScoredChunk> scored = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            double similarity = cosineSimilarity(queryVector, embeddings.get(i));
            if (similarity >= similarityThreshold) {
                scored.add(new ScoredChunk(chunks.get(i), similarity));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return scored.size() > topK ? scored.subList(0, topK) : scored;
    }

    public List<Chunk> allChunks() {
        return List.copyOf(chunks);
    }

    static double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
