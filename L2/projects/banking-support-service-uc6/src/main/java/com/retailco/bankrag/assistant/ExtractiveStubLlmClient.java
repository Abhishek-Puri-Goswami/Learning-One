package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.Chunker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// CONCEPT: "Extractive" answer generation -- a rule-based algorithm that
// mimics an LLM's output shape without any real language model.
// PURPOSE: The automatic offline fallback for LlmClient, used whenever
// OpenAiLlmClient.isConfigured() is false. It lets the full pipeline
// (retrieval -> prompt -> "generation" -> citation -> guardrail -> trace
// -> evaluation) run end-to-end with zero network calls and zero API key.
//
// HOW IT WORKS (see generate() below, step by step):
// 1. Pull the QUESTION and the retrieved CONTEXT chunks back out of the
//    already-built prompt string (extractSection/extractContextBlocks --
//    simple string parsing, since this class never sees structured data,
//    only the final prompt text PromptTemplate produced).
// 2. Split each context chunk into sentences.
// 3. Score every sentence by how many of the question's meaningful words
//    (after removing STOPWORDS like "the", "is", "a") it contains.
// 4. Keep the highest-scoring sentences (capped per chunk, so one chunk
//    can't dominate the answer) and join them into a final answer string,
//    each one tagged with its source [chunk-id] -- inline citations.
//
// WHY "extractive" rather than free-form text generation: it only ever
// copies real sentences straight from the retrieved chunks, so its output
// is trustworthy by construction -- there's no way for it to invent facts
// the way a real LLM might hallucinate. IMPORTANT trade-off: it can't
// paraphrase, summarize across sentences, or reason -- its answer quality
// is not representative of a real LLM's (see the sibling OpenAiLlmClient
// for the real generative implementation).
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
