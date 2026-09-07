package com.retailco.bankrag.core;

// CONCEPT: Strategy pattern, expressed as a Java interface.
// PURPOSE:
// Defines the one contract every embedding implementation must satisfy:
// turn text into a fixed-length numeric vector (embed) and report that
// vector's length (dimensions). Nothing else in the codebase is allowed
// to know HOW the vector is produced -- only that it can be produced.
//
// WHY an interface here: this is what lets the rest of the app (VectorStore,
// Spring @Configuration classes) depend on "an EmbeddingModel" in the
// abstract, while the concrete choice -- OpenAiEmbeddingModel (real) vs.
// LocalHashingEmbeddingModel (offline stand-in) -- is decided in exactly
// ONE place (see each module's *Config.java). Swapping implementations
// never requires touching VectorStore, Chunker, or any business logic.
//
// IMPORTANT (L2 reference guide 3.1, "Same Embedding Model Requirement"):
// whichever implementation is chosen MUST be used for BOTH indexing chunks
// and embedding user queries. Cosine similarity between vectors produced
// by two different embedding models is meaningless -- see VectorStore's
// semanticSearch, which always calls embeddingModel.embed() for the query
// using the exact same instance that indexed the chunks.
//
// SPRING BOOT CONCEPT TO LEARN: this interface is what Spring calls
// "programming to an interface" -- the @Bean methods in *Config.java
// decide which concrete class gets injected wherever `EmbeddingModel` is
// a constructor/method parameter.
public interface EmbeddingModel {
    double[] embed(String text);

    int dimensions();
}
