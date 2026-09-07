package com.retailco.bankrag.core;

/**
 * Chunking configuration, per L2's reference guide recommendation of
 * 300-1200 tokens with 100-150 overlap (see design/chunking-configuration.md
 * for the full rationale and the values actually chosen for this corpus).
 * Token counts here use a simple whitespace-token approximation (see
 * Chunker.tokenize) rather than a real BPE tokenizer, since no LLM-vendor
 * tokenizer library was reachable in this sandbox (Maven Central blocked) --
 * documented explicitly as a stand-in, not a claim of exact token-parity
 * with a real model's tokenizer.
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
