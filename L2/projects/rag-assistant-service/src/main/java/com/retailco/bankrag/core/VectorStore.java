package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// CONCEPT: In-memory vector database (a dev/local stand-in for a real
// vector store like pgvector, Pinecone, or Weaviate).
// PURPOSE: Stores every indexed Chunk alongside its embedding vector, and
// answers "which chunks are most similar to this query?" using cosine
// similarity -- the core retrieval step of RAG.
//
// FLOW: Chunker produces Chunks -> index(chunk) embeds and stores each one
// -> semanticSearch(query, ...) embeds the query the SAME way and ranks
// every stored chunk by similarity -> the top matches get sent to the LLM
// as grounding context (see RagAssistant in the assistant/ package).
//
// WHY in-memory: two parallel lists (`chunks` and `embeddings`, same index
// = same chunk) keep this class dependency-free so the whole pipeline is
// runnable without a database. A production system would replace this
// class's storage with a real vector database, but the semanticSearch
// contract (query in, ranked ScoredChunks out) would stay the same --
// that's the value of hiding this behind a class boundary.
//
// IMPORTANT: cosineSimilarity only makes sense when the query and the
// chunks were embedded by the exact same EmbeddingModel instance -- see
// EmbeddingModel's Javadoc for why.
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
