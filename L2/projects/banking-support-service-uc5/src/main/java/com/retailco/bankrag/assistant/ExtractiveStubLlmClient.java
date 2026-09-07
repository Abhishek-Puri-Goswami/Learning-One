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
 * Deliberate, disclosed stand-in for a real generative LLM (GPT-4/Claude/
 * Gemini/etc.) -- this sandbox has no reachable network egress to any LLM
 * API and no API key configured, so a true generative call cannot be made
 * or verified here (same class of limitation as UC1's LocalHashingEmbeddingModel
 * standing in for a real embedding provider -- see that class's Javadoc for
 * the identical reasoning).
 *
 * What this class DOES do, for real: given the exact prompt PromptTemplate
 * builds (system instructions + retrieved CONTEXT chunks + question), it
 * performs extractive answer construction -- it scores every sentence in
 * the retrieved context by lexical overlap with the question, selects the
 * highest-scoring sentences (respecting a per-chunk cap so one chunk can't
 * dominate), and stitches them together with inline [chunk-id] citations
 * exactly as SYSTEM_INSTRUCTIONS rule 2 requires. This is NOT abstractive
 * generation and will not paraphrase or reason the way a real LLM does --
 * but it DOES let the full pipeline (retrieval -> prompt -> "generation" ->
 * citation -> guardrail -> trace -> evaluation) actually run end-to-end and
 * be measured for real in this sandbox, rather than only being described on
 * paper. See README.md's "What's real vs. documented" section and
 * design/generation-module.md for the production swap-in path.
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

    private int estimateTokens(String text) {
        // Whitespace-token approximation, consistent with rag-core's Chunker
        // (see design/chunking-configuration.md's disclosed limitation --
        // same rationale applies to token/cost estimation in evaluation/).
        return Chunker.tokenize(text).length;
    }

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "what", "how", "do", "does", "i",
            "my", "of", "for", "to", "in", "on", "and", "or", "it", "this", "that", "can",
            "should", "will", "be", "if", "with", "as");
}
