package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ScoredChunk;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A lightweight, automatic way to score how good an answer actually was —
 * without needing a human (or another AI) to review it. It measures two
 * things: faithfulness (does the answer actually say what its cited
 * sources say?) and relevance (was the retrieved information even related
 * to the question?).
 * <p>
 * Here's how faithfulness is computed, step by step (see
 * {@code computeFaithfulness()} below — this is the more interesting
 * metric of the two):
 * <ol>
 *   <li>Split the answer into individual sentences.</li>
 *   <li>For each sentence that has a "[chunk-id]" citation attached,
 *       find that chunk's original text.</li>
 *   <li>Work out what fraction of the sentence's own words also appear
 *       in that cited chunk's text.</li>
 *   <li>Average that fraction across every cited sentence.</li>
 * </ol>
 * A more sophisticated system might use another AI model to judge whether
 * a source truly SUPPORTS a claim, even if worded differently. This
 * simpler word-overlap version is a rougher approximation of the same
 * idea, but it's free and runs instantly, with no extra AI calls needed.
 * <p>
 * Relevance is simply the top retrieved chunk's similarity score — a
 * cheap, already-available signal for "did we even find the right
 * information?"
 * <p>
 * One important edge case: an answer with NO citations at all is only
 * treated as faithful if it looks like an honest "I don't know" — any
 * other uncited answer scores 0, since an assistant making an uncited
 * claim is exactly the kind of problem this metric exists to catch.
 */
public final class EvaluationHarness {

    public record EvaluationResult(
            double faithfulnessScore,   // fraction of answer's citation claims backed by cited chunk text
            double relevanceScore,      // top retrieved chunk's similarity score (proxy for retrieval relevance)
            long latencyMs,
            int totalTokens
    ) {
    }

    public EvaluationResult evaluate(String query, String finalAnswer,
                                      List<ScoredChunk> retrievedChunks,
                                      List<CitationExtractor.Citation> citations,
                                      long latencyMs, int promptTokens, int completionTokens) {
        double faithfulness = computeFaithfulness(finalAnswer, citations, retrievedChunks);
        double relevance = retrievedChunks.isEmpty() ? 0.0 : retrievedChunks.get(0).score();
        return new EvaluationResult(faithfulness, relevance, latencyMs, promptTokens + completionTokens);
    }

    /**
     * Faithfulness proxy: for every [chunk-id]-cited sentence in the answer,
     * check what fraction of the sentence's own content terms actually
     * appear in the cited chunk's text. A real faithfulness judge (LangSmith
     * "groundedness" evaluator) would use an LLM to check semantic
     * entailment, not lexical overlap -- this is a coarser but real,
     * computable stand-in, consistent with the whitespace-tokenizer
     * approximation used throughout this submission (see UC1's
     * design/chunking-configuration.md).
     */
    private double computeFaithfulness(String answer, List<CitationExtractor.Citation> citations,
                                        List<ScoredChunk> retrievedChunks) {
        if (citations.isEmpty()) {
            // No citations at all only counts as faithful if the answer
            // looks like an honest "I don't know" — that's a correct
            // decline, not an unfaithful answer. Any other uncited answer
            // scores 0.
            return answer.toLowerCase().contains("don't have") || answer.toLowerCase().contains("does not contain")
                    ? 1.0 : 0.0;
        }

        java.util.Map<String, String> chunkTextById = new java.util.HashMap<>();
        for (ScoredChunk sc : retrievedChunks) {
            chunkTextById.put(sc.chunk().id(), sc.chunk().text());
        }

        // This regex is written carefully so a trailing "[chunk-id]"
        // citation stays attached to the sentence it follows, instead of
        // being split off into its own separate fragment (which would
        // leave that fragment with no real words to check faithfulness
        // against, and leave the actual sentence with no citation to
        // check it against).
        String[] sentences = answer.split("(?<=[.!?])\\s+(?!\\[)");
        double totalScore = 0;
        int checkedSentences = 0;

        for (String sentence : sentences) {
            String cited = citedChunkIdIn(sentence, citations);
            if (cited == null) continue;
            String chunkText = chunkTextById.get(cited);
            if (chunkText == null) continue; // an unresolvable citation is skipped, not rewarded, in the average
            Set<String> sentenceTerms = toTermSet(sentence);
            Set<String> chunkTerms = toTermSet(chunkText);
            if (sentenceTerms.isEmpty()) continue;
            long overlap = sentenceTerms.stream().filter(chunkTerms::contains).count();
            totalScore += (double) overlap / sentenceTerms.size();
            checkedSentences++;
        }

        return checkedSentences == 0 ? 0.0 : totalScore / checkedSentences;
    }

    private String citedChunkIdIn(String sentence, List<CitationExtractor.Citation> citations) {
        for (CitationExtractor.Citation c : citations) {
            if (sentence.contains("[" + c.chunkId() + "]")) {
                return c.chunkId();
            }
        }
        return null;
    }

    private Set<String> toTermSet(String text) {
        String cleaned = text.replaceAll("\\[[^\\[\\]]+#\\d+]", ""); // strip citation markers themselves
        return new HashSet<>(Arrays.asList(Chunker.tokenize(cleaned.toLowerCase())));
    }
}
