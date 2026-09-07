package com.retailco.bankrag.core;

/**
 * A single retrievable unit stored in the vector database.
 * Mirrors the "vector_store.save(vector, metadata={chunk_text, source, page_number})"
 * pattern described in L2's Building RAG Systems reference guide, and the
 * VectorStore schema in design/vector-database-schema.sql.
 */
public record Chunk(String id, String sourceDocument, int chunkIndex, String text) {
}
