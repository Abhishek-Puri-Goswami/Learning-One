package com.retailco.bankrag.core;

// CONCEPT: Domain model / Value Object, implemented as a Java `record`.
// PURPOSE:
// A Chunk is the smallest unit of text the RAG (Retrieval-Augmented
// Generation) pipeline works with. A long document is split into many
// Chunks (see Chunker), each one gets its own embedding vector, and at
// query time the system retrieves the most relevant Chunks instead of
// searching the whole document.
//
// WHY a `record`:
// A `record` is just a Java class that automatically gets a constructor,
// getters (id(), text(), ...), equals(), hashCode(), and toString(). It's
// the right tool here because a Chunk is pure, immutable data with no
// behavior -- exactly what records are designed for.
//
// FLOW:
// DocumentLoader reads raw text -> Chunker splits it into Chunk objects ->
// VectorStore embeds and stores each Chunk -> at search time, the closest
// Chunks are returned and cited back to the user.
//
// IMPORTANT: `id` (e.g. "policy.txt#3") is what gets shown to the user as
// a citation, so retrieval results can always be traced back to their
// source document and position.
public record Chunk(String id, String sourceDocument, int chunkIndex, String text) {
}
