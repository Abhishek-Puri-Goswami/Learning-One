package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ScoredChunk;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// CONCEPT: Automated evaluation / "LLM-as-judge" proxy metrics -- a
// lightweight, code-only stand-in for what an LLM-based evaluator would do.
// PURPOSE: Scores every answer on two axes: faithfulness (does the answer
// actually say what its cited sources say?) and relevance (was the
// retrieved context actually related to the question?). This is what lets
// you measure RAG quality automatically instead of eyeballing outputs.
//
// HOW faithfulness is computed (see computeFaithfulness() below, step by
// step -- this is the more interesting metric):
// 1. Split the answer into sentences.
// 2. For each sentence that carries a "[chunk-id]" citation, find that
//    chunk's original text.
// 3. Compute what fraction of the sentence's own words also appear in
//    the cited chunk's text (lexical overlap, same technique as
//    KeywordSearcher/ExtractiveStubLlmClient).
// 4. Average that fraction across all cited sentences.
// A real "groundedness" evaluator would use another LLM to judge semantic
// entailment (does the source actually SUPPORT this claim, even if worded
// differently?); this lexical-overlap version is a coarser, fully local,
// zero-cost approximation of the same idea.
//
// WHY relevance is just "the top chunk's similarity score": it's a cheap,
// already-computed proxy for "was the right information even found?" --
// no additional computation needed, since VectorStore already produces it.
//
// IMPORTANT edge case (see the citations.isEmpty() branch): an answer with
// NO citations is only treated as faithful (score 1.0) if it looks like a
// fallback/decline ("I don't have...", "does not contain..."). Otherwise
// it scores 0.0 -- a grounded assistant making an uncited claim is exactly
// the failure mode this metric exists to catch.
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
            // No citations at all is itself a faithfulness problem for a
            // grounded-answer requirement, unless the answer is a fallback
            // ("I don't know") -- callers should interpret a 0.0 score with
            // an empty citation list plus a fallback-shaped answer as
            // "correctly declined," not "unfaithful."
            return answer.toLowerCase().contains("don't have") || answer.toLowerCase().contains("does not contain")
                    ? 1.0 : 0.0;
        }

        java.util.Map<String, String> chunkTextById = new java.util.HashMap<>();
        for (ScoredChunk sc : retrievedChunks) {
            chunkTextById.put(sc.chunk().id(), sc.chunk().text());
        }

        // Negative lookahead (?!\[) keeps a trailing "[chunk-id]" citation
        // attached to the sentence it follows, rather than splitting it into
        // its own citation-only fragment (which would leave that fragment
        // with no content terms to check faithfulness against, and leave the
        // actual claim sentence with no citation to check it against --
        // found via this test actually failing on a legitimately faithful
        // answer before this fix, not assumed).
        String[] sentences = answer.split("(?<=[.!?])\\s+(?!\\[)");
        double totalScore = 0;
        int checkedSentences = 0;

        for (String sentence : sentences) {
            String cited = citedChunkIdIn(sentence, citations);
            if (cited == null) continue;
            String chunkText = chunkTextById.get(cited);
            if (chunkText == null) continue; // unresolvable citation -- excluded from the average, not rewarded
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
