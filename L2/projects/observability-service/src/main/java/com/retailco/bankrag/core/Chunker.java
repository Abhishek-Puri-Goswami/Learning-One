package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits one long document into many smaller, slightly-overlapping
 * pieces called Chunks. Here's how, step by step:
 * <ol>
 *   <li>Split the whole document into individual words ({@code tokenize()}).</li>
 *   <li>Slide a "window" of {@code chunkSizeTokens} words across that
 *       list, one chunk at a time.</li>
 *   <li>After each chunk, move forward by
 *       {@code chunkSizeTokens - overlapTokens} words — NOT by the full
 *       chunk size. That's exactly what makes each chunk share a little
 *       text with the one before it.</li>
 *   <li>Stop once we reach the end of the document.</li>
 * </ol>
 * <p>
 * Why bother with overlap at all? Imagine a sentence like
 * "...the interest rate is 8.25%..." happens to fall right at the edge
 * between two chunks — without overlap, that fact could be split in half
 * and neither chunk alone would contain the whole sentence. With overlap,
 * that sentence is very likely to appear in full in at least one chunk.
 * <p>
 * If you removed the overlap entirely (set it to 0), each chunk would sit
 * right after the previous one with no shared text — simpler, but facts
 * sitting near a chunk boundary would become harder to find correctly.
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
