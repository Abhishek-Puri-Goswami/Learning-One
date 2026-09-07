package com.retailco.bankrag.core;

/**
 * Holds the two settings that control how documents get split into
 * chunks: how big each chunk is ({@code chunkSizeTokens}) and how much
 * consecutive chunks overlap ({@code overlapTokens}). The overlap exists
 * so a sentence that happens to fall right on a chunk boundary still
 * appears complete in at least one chunk, instead of being cut in half.
 * <p>
 * Notice the block below labeled {@code public ChunkingConfig { ... }} —
 * that's a record's "compact constructor," and it runs automatically
 * every single time one of these objects gets created, no matter how.
 * Putting our validation there means an invalid {@code ChunkingConfig}
 * (like a negative size) simply can never exist anywhere in the program —
 * it gets rejected the moment someone tries to create one.
 * <p>
 * One thing to keep in mind: these token counts are approximate (see
 * {@code Chunker.tokenize}, which just splits on whitespace rather than
 * using a real AI-model tokenizer), so treat them as "roughly how much
 * text," not an exact number.
 */
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
