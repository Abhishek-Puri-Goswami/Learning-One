package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.HybridSearcher;
import com.retailco.bankrag.core.KeywordSearcher;
import com.retailco.bankrag.core.ScoredChunk;
import com.retailco.bankrag.core.VectorStore;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

// CONCEPT: Orchestrator / Facade -- the central class that ties every
// other piece in this package together into one pipeline. This is the
// class you should read FIRST to understand how the whole RAG assistant
// fits together; everything else in this package exists to support one
// step of ask() below.
//
// PURPOSE: Given a raw user query, produce a safe, grounded, cited
// AssistantResponse -- or a clear reason why it couldn't.
//
// FLOW (see ask() below -- this is the full pipeline, step by step):
//   1. PromptInjectionGuard checks the query for injection attempts.
//      Blocked here -> return immediately, no retrieval, no LLM call.
//   2. UnsafeQueryGuard checks for out-of-scope advice-seeking questions.
//      Blocked here -> return immediately, same reasoning.
//   3. HybridSearcher retrieves the top-K most relevant chunks (reusing
//      UC1's VectorStore/KeywordSearcher).
//   4. The retrieval guardrail checks whether the top result is both
//      above `similarityThreshold` AND clearly ahead of the runner-up by
//      `minScoreMargin`. If not ("weak retrieval"), return a fallback
//      answer instead of letting the LLM guess from thin evidence.
//   5. PromptTemplate builds the full prompt from the query + retrieved
//      chunks, and llmClient.generate(prompt) produces the answer text.
//   6. CitationExtractor pulls structured citations out of that answer.
//   7. EvaluationHarness scores the answer's faithfulness/relevance.
//   8. TraceLogger records everything about this run (query, prompt,
//      answer, citations, guardrail decisions, latency, tokens) as one
//      JSON line -- a local stand-in for LangSmith-style observability.
//
// WHY guardrails run BEFORE retrieval/generation: both are cheap checks
// (no network calls), so a blocked query costs nothing -- no wasted
// embedding call, no wasted (and possibly paid) LLM call.
//
// WHY the retrieval guardrail uses BOTH a threshold AND a margin: a high
// absolute similarity score can still be misleading if a completely
// unrelated chunk happens to score almost as high (ambiguous evidence) --
// requiring the top result to also be clearly ahead of the runner-up
// catches that case, which a threshold alone would miss.
//
// WHAT IF REMOVED: without this class, every other component here
// (guards, retrieval, prompt building, citation extraction, evaluation,
// tracing) would still work individually, but nothing would coordinate
// them into one safe, observable request/response cycle -- callers
// (Spring controllers) would have to reimplement this orchestration
// themselves, and likely get the ordering (guardrails BEFORE cost) wrong.
public class RagAssistant {

    private final VectorStore vectorStore;
    private final HybridSearcher hybridSearcher;
    private final LlmClient llmClient;
    private final TraceLogger traceLogger;
    private final EvaluationHarness evaluationHarness = new EvaluationHarness();
    private final PromptInjectionGuard promptInjectionGuard = new PromptInjectionGuard();
    private final UnsafeQueryGuard unsafeQueryGuard = new UnsafeQueryGuard();

    private final double similarityThreshold;
    private final double minScoreMargin;
    private final int topK;

    public RagAssistant(VectorStore vectorStore, LlmClient llmClient, TraceLogger traceLogger,
                         double semanticWeight, double keywordWeight,
                         double similarityThreshold, double minScoreMargin, int topK) {
        this.vectorStore = vectorStore;
        this.hybridSearcher = new HybridSearcher(vectorStore, new KeywordSearcher(), semanticWeight, keywordWeight);
        this.llmClient = llmClient;
        this.traceLogger = traceLogger;
        this.similarityThreshold = similarityThreshold;
        this.minScoreMargin = minScoreMargin;
        this.topK = topK;
    }

    public record AssistantResponse(
            String query,
            String answer,
            List<CitationExtractor.Citation> citations,
            boolean blocked,
            String blockReason,
            boolean fallback,
            EvaluationHarness.EvaluationResult evaluation
    ) {
    }

    public AssistantResponse ask(String query) {
        String runId = TraceLogger.TraceRecord.newRunId();
        long start = System.currentTimeMillis();

        // Step 1a: prompt-injection guardrail
        PromptInjectionGuard.Verdict injectionVerdict = promptInjectionGuard.check(query);
        if (injectionVerdict.blocked()) {
            return blockedResponse(runId, query, start, true, false, injectionVerdict.reason());
        }

        // Step 1b: unsafe/advice-seeking guardrail
        UnsafeQueryGuard.Verdict unsafeVerdict = unsafeQueryGuard.check(query);
        if (unsafeVerdict.blocked()) {
            return blockedResponse(runId, query, start, false, true, unsafeVerdict.reason());
        }

        // Step 2: retrieval
        List<ScoredChunk> retrieved = hybridSearcher.search(query, topK, 0.0);

        // Step 3: retrieval guardrail (threshold + score-margin, per UC1's hallucination-risk-analysis.md)
        Double topScore = retrieved.isEmpty() ? null : retrieved.get(0).score();
        Double margin = retrieved.size() >= 2 ? retrieved.get(0).score() - retrieved.get(1).score() : null;
        boolean weakRetrieval = topScore == null || topScore < similarityThreshold
                || (margin != null && margin < minScoreMargin);

        if (weakRetrieval) {
            String fallbackAnswer = "I don't have enough relevant, verified policy information to "
                    + "confidently answer this question. Please rephrase, or contact Secure Bank support "
                    + "for questions outside the policy manual's scope.";
            long latency = System.currentTimeMillis() - start;
            traceLogger.log(new TraceLogger.TraceRecord(runId, "chain", query, retrieved,
                    "(prompt not sent to LLM -- retrieval guardrail fired before generation)",
                    null, fallbackAnswer, List.of(), false, false, true,
                    "Weak retrieval: topScore=" + topScore + " margin=" + margin, latency,
                    0, 0, null, Instant.now()));
            EvaluationHarness.EvaluationResult eval = new EvaluationHarness.EvaluationResult(
                    1.0, topScore == null ? 0.0 : topScore, latency, 0);
            return new AssistantResponse(query, fallbackAnswer, List.of(), false, null, true, eval);
        }

        // Step 4: prompt construction + generation
        String prompt = PromptTemplate.build(query, retrieved);
        LlmClient.LlmResponse llmResponse = llmClient.generate(prompt);

        // Step 5: citation extraction
        List<CitationExtractor.Citation> citations = CitationExtractor.extract(llmResponse.rawText(), retrieved);

        long latency = System.currentTimeMillis() - start;

        // Step 7: evaluation
        EvaluationHarness.EvaluationResult eval = evaluationHarness.evaluate(query, llmResponse.rawText(),
                retrieved, citations, latency, llmResponse.estimatedPromptTokens(), llmResponse.estimatedCompletionTokens());

        // Step 6: trace log
        traceLogger.log(new TraceLogger.TraceRecord(runId, "chain", query, retrieved, prompt,
                llmResponse.rawText(), llmResponse.rawText(), citations, false, false, false, null,
                latency, llmResponse.estimatedPromptTokens(), llmResponse.estimatedCompletionTokens(),
                null, Instant.now()));

        return new AssistantResponse(query, llmResponse.rawText(), citations, false, null, false, eval);
    }

    private AssistantResponse blockedResponse(String runId, String query, long start,
                                               boolean injectionBlocked, boolean unsafeBlocked, String reason) {
        String answer = "This request cannot be processed: " + reason;
        long latency = System.currentTimeMillis() - start;
        traceLogger.log(new TraceLogger.TraceRecord(runId, "chain", query, List.of(),
                "(prompt not built -- blocked pre-retrieval)", null, answer, List.of(),
                injectionBlocked, unsafeBlocked, false, reason, latency, 0, 0, null, Instant.now()));
        EvaluationHarness.EvaluationResult eval = new EvaluationHarness.EvaluationResult(1.0, 0.0, latency, 0);
        return new AssistantResponse(query, answer, List.of(), true, reason, false, eval);
    }

    public static Path defaultTraceLogPath() {
        return Path.of("reports", "langsmith-style-trace-log.jsonl");
    }
}
