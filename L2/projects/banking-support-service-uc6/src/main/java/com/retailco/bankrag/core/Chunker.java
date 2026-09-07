package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Sliding-window chunker: splits a document's whitespace-tokenized text into
 * overlapping windows, per the L2 reference guide's "chunks = split_text(text,
 * chunk_size=500, overlap=100)" pseudo-code -- implemented for real here
 * rather than left as pseudo-code.
 */
public class Chunker {

    private final ChunkingConfig config;

    public Chunker(ChunkingConfig config) {
        this.config = config;
    }

    public List<Chunk> chunk(String sourceDocument, String text) {
        String[] tokens = tokenize(text);
        List<Chunk> chunks = new ArrayList<>();

        if (tokens.length == 0) {
            return chunks;
        }

        int step = config.chunkSizeTokens() - config.overlapTokens();
        int chunkIndex = 0;

        for (int start = 0; start < tokens.length; start += step) {
            int end = Math.min(start + config.chunkSizeTokens(), tokens.length);
            String chunkText = String.join(" ", java.util.Arrays.asList(tokens).subList(start, end));
            chunks.add(new Chunk(sourceDocument + "#" + chunkIndex, sourceDocument, chunkIndex, chunkText));
            chunkIndex++;

            if (end == tokens.length) {
                break; // last window already reached the end of the document
            }
        }

        return chunks;
    }

    /**
     * Whitespace tokenization -- a deliberately simple stand-in for a real
     * subword/BPE tokenizer (see ChunkingConfig's Javadoc for why). Adequate
     * for measuring "roughly how much text is in a chunk," not exact enough
     * to claim parity with any specific LLM's token accounting.
     */
    public static String[] tokenize(String text) {
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return new String[0];
        }
        return normalized.split(" ");
    }
}
