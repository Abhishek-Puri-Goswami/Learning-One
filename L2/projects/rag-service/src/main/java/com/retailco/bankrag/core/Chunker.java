package com.retailco.bankrag.core;

import java.util.ArrayList;
import java.util.List;

// CONCEPT: Sliding-window algorithm.
// PURPOSE:
// Turns one long document's text into many overlapping Chunk objects, so
// each Chunk is small enough to embed and retrieve independently.
//
// HOW IT WORKS (step by step):
// 1. Split the whole text into individual word-tokens (tokenize()).
// 2. Walk a "window" of `chunkSizeTokens` tokens across that list.
// 3. After each window, move the start position forward by
//    `step = chunkSizeTokens - overlapTokens` tokens, NOT by the full
//    chunk size -- this is what creates the overlap between chunk N and
//    chunk N+1.
// 4. Stop once a window reaches the end of the document.
//
// WHY overlap matters: without it, a sentence that straddles a chunk
// boundary (e.g. "...interest rate is | 8.25%...") would be split across
// two chunks and neither chunk alone would contain the full fact. With
// overlap, that sentence is likely to appear whole in at least one chunk.
//
// WHAT IF REMOVED/CHANGED: dropping overlap (setting it to 0) would make
// `step == chunkSizeTokens`, so chunks would tile the document with no
// repeated text -- simpler, but retrieval quality would drop for facts
// that sit near a chunk boundary.
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
