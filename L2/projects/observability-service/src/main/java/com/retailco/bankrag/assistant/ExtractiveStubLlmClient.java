package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.Chunker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This is our OFFLINE fallback for generating answers, automatically used
 * whenever no OpenAI API key is configured. It lets the ENTIRE pipeline —
 * from retrieving documents all the way to citing sources — run
 * end-to-end without any network call or API key at all.
 * <p>
 * It's called "extractive" because instead of writing new sentences the
 * way a real AI model would, it only ever copies real sentences straight
 * out of the retrieved documents. Here's how, step by step (see
 * {@code generate()} below):
 * <ol>
 *   <li>Pull the question and the retrieved context chunks back out of
 *       the already-built prompt text.</li>
 *   <li>Split each chunk of context into individual sentences.</li>
 *   <li>Score every sentence by how many of the question's meaningful
 *       words it contains (ignoring common filler words like "the" or
 *       "is").</li>
 *   <li>Keep only the best-scoring sentences, and join them together
 *       into a final answer, tagging each one with which source
 *       [chunk-id] it came from.</li>
 * </ol>
 * <p>
 * Because it only ever copies real text, this stand-in can never invent
 * facts the way a real AI model occasionally can. The trade-off: it can't
 * paraphrase, summarize across sentences, or reason about the text — its
 * answer quality is nowhere near what a real AI model produces (see the
 * sibling class {@code OpenAiLlmClient} for the real one).
 */
public class ExtractiveStubLlmClient implements LlmClient {

    private static final int MAX_SENTENCES_PER_CHUNK = 2;
    private static final int MAX_TOTAL_SENTENCES = 4;
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    @Override
    public LlmResponse generate(String prompt) {
        String question = extractSection(prompt, "QUESTION:\n", "\n\nANSWER");
        List<ContextBlock> blocks = extractContextBlocks(prompt);

        Set<String> queryTerms = toTermSet(question);
        if (blocks.isEmpty() || queryTerms.isEmpty()) {
            String answer = "I don't have relevant policy information to answer this question.";
            return new LlmResponse(answer, estimateTokens(prompt), estimateTokens(answer));
        }

        List<ScoredSentence> candidates = new ArrayList<>();
        for (ContextBlock block : blocks) {
            String[] sentences = SENTENCE_SPLIT.split(block.text().trim());
            int taken = 0;
            List<ScoredSentence> perChunk = new ArrayList<>();
            for (String sentence : sentences) {
                String trimmed = sentence.trim();
                if (trimmed.isEmpty()) continue;
                Set<String> sentenceTerms = toTermSet(trimmed);
                long overlap = queryTerms.stream().filter(sentenceTerms::contains).count();
                double score = queryTerms.isEmpty() ? 0 : (double) overlap / queryTerms.size();
                if (score > 0) {
                    perChunk.add(new ScoredSentence(trimmed, block.chunkId(), score));
                }
            }
            perChunk.sort(Comparator.comparingDouble(ScoredSentence::score).reversed());
            for (ScoredSentence s : perChunk) {
                if (taken >= MAX_SENTENCES_PER_CHUNK) break;
                candidates.add(s);
                taken++;
            }
        }

        candidates.sort(Comparator.comparingDouble(ScoredSentence::score).reversed());

        if (candidates.isEmpty()) {
            String answer = "The retrieved policy context does not contain information "
                    + "that directly answers this question.";
            return new LlmResponse(answer, estimateTokens(prompt), estimateTokens(answer));
        }

        List<ScoredSentence> selected = candidates.size() > MAX_TOTAL_SENTENCES
                ? candidates.subList(0, MAX_TOTAL_SENTENCES) : candidates;

        StringBuilder answer = new StringBuilder();
        for (ScoredSentence s : selected) {
            answer.append(s.text());
            if (!s.text().endsWith(".") && !s.text().endsWith(":")) {
                answer.append(".");
            }
            answer.append(" [").append(s.chunkId()).append("] ");
        }

        String finalAnswer = answer.toString().trim();
        return new LlmResponse(finalAnswer, estimateTokens(prompt), estimateTokens(finalAnswer));
    }

    @Override
    public String modelId() {
        return "extractive-stub-v1 (NOT a production LLM -- see class Javadoc)";
    }

    private record ContextBlock(String chunkId, String text) {
    }

    private record ScoredSentence(String text, String chunkId, double score) {
    }

    private List<ContextBlock> extractContextBlocks(String prompt) {
        List<ContextBlock> blocks = new ArrayList<>();
        String context = extractSection(prompt, "CONTEXT:\n", "\nQUESTION:");
        if (context == null || context.contains("(no relevant context retrieved)")) {
            return blocks;
        }
        String[] parts = context.split("--- \\[");
        for (String part : parts) {
            if (part.isBlank()) continue;
            int idEnd = part.indexOf(']');
            if (idEnd < 0) continue;
            String chunkId = part.substring(0, idEnd);
            int textStart = part.indexOf("---\n");
            if (textStart < 0) continue;
            String text = part.substring(textStart + 4).trim();
            blocks.add(new ContextBlock(chunkId, text));
        }
        return blocks;
    }

    private String extractSection(String prompt, String startMarker, String endMarker) {
        int start = prompt.indexOf(startMarker);
        if (start < 0) return null;
        start += startMarker.length();
        int end = prompt.indexOf(endMarker, start);
        if (end < 0) end = prompt.length();
        return prompt.substring(start, end);
    }

    private Set<String> toTermSet(String text) {
        String[] tokens = Chunker.tokenize(text.toLowerCase());
        Set<String> terms = new LinkedHashSet<>(Arrays.asList(tokens));
        terms.removeIf(STOPWORDS::contains);
        return terms;
    }

    /**
     * A rough approximation of "how many tokens is this text," done by
     * just counting words split on whitespace — the same simple method
     * {@code Chunker} uses. It's not exactly how a real AI model counts
     * tokens, but it's close enough to be a useful estimate.
     */
    private int estimateTokens(String text) {
        return Chunker.tokenize(text).length;
    }

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "what", "how", "do", "does", "i",
            "my", "of", "for", "to", "in", "on", "and", "or", "it", "this", "that", "can",
            "should", "will", "be", "if", "with", "as");
}
