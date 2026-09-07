package com.retailco.bankrag.core;

// CONCEPT: Immutable configuration object with self-validation (compact
// constructor), plus a "record pattern" for value-object validation.
// PURPOSE:
// Holds the two tunable knobs for splitting documents into chunks:
// how big each chunk is (chunkSizeTokens) and how much consecutive chunks
// overlap (overlapTokens). Overlap exists so a sentence that gets cut at a
// chunk boundary still appears in full inside at least one chunk.
//
// WHY validate here (in the compact constructor below):
// A `record`'s compact constructor `public ChunkingConfig { ... }` runs
// before the fields are assigned, for every way the record can be built.
// Putting the validation here (rather than in each caller) guarantees an
// invalid ChunkingConfig can never exist anywhere in the program --
// "make illegal states unrepresentable."
//
// IMPORTANT: token counts are approximate (see Chunker.tokenize -- simple
// whitespace splitting, not a real BPE/subword tokenizer), so treat these
// numbers as "roughly how much text," not an exact LLM token budget.
public record ChunkingConfig(int chunkSizeTokens, int overlapTokens) {

    public ChunkingConfig {
        if (chunkSizeTokens <= 0) {
            throw new IllegalArgumentException("chunkSizeTokens must be positive");
        }
        if (overlapTokens < 0 || overlapTokens >= chunkSizeTokens) {
            throw new IllegalArgumentException("overlapTokens must be >= 0 and < chunkSizeTokens");
        }
    }

    /** The chosen default for this policy-manual corpus -- see design/chunking-configuration.md. */
    public static ChunkingConfig defaultConfig() {
        return new ChunkingConfig(180, 40);
    }
}
