package com.retailco.bankrag.core;

/**
 * A Chunk is the smallest piece of text our RAG (Retrieval-Augmented
 * Generation) pipeline works with. Instead of searching through a whole
 * document, we split it into many small Chunks (see {@link Chunker}),
 * turn each one into a numeric vector, and at search time only look at
 * the handful of Chunks most relevant to the question being asked.
 * <p>
 * This is written as a Java {@code record}, which is just a compact way
 * to write a class that only holds data and has no real behavior of its
 * own — Java automatically generates the constructor and getters for us.
 * <p>
 * Here's the journey a piece of text takes: {@code DocumentLoader} reads
 * the raw file → {@code Chunker} splits it into Chunks → {@code VectorStore}
 * turns each Chunk into a vector and stores it → when a question comes
 * in, the most relevant Chunks are found and shown back as the answer's
 * sources.
 * <p>
 * The {@code id} field (something like {@code "policy.txt#3"}) is what
 * gets shown to the user as a citation, so every answer can always be
 * traced back to exactly which document and section it came from.
 */
public record Chunk(String id, String sourceDocument, int chunkIndex, String text) {
}
