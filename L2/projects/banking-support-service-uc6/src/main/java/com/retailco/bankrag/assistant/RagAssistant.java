package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.HybridSearcher;
import com.retailco.bankrag.core.KeywordSearcher;
import com.retailco.bankrag.core.ScoredChunk;
import com.retailco.bankrag.core.VectorStore;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * This is the heart of the whole RAG assistant — the class that ties
 * every other piece in this package together into one working pipeline.
 * Read this class first; everything else here exists to support one step
 * of the {@link #ask} method below.
 * <p>
 * Given a raw question from a user, this class's job is to produce a
 * safe, grounded, properly-cited answer — or a clear reason why it
 * couldn't. Here's the full pipeline, step by step:
 * <ol>
 *   <li>{@code PromptInjectionGuard} checks the question for injection
 *       attempts. If blocked, we return right away — no document lookup,
 *       no AI call.</li>
 *   <li>{@code UnsafeQueryGuard} checks for out-of-scope
 *       advice-seeking questions. Blocked here too? Same early return.</li>
 *   <li>{@code HybridSearcher} retrieves the most relevant document
 *       chunks for the question.</li>
 *   <li>A "weak retrieval" check makes sure the top result is both above
 *       a minimum similarity score AND clearly better than the runner-up.
 *       If not, we return an honest "I don't know" fallback instead of
 *       letting the AI guess from thin evidence.</li>
 *   <li>{@code PromptTemplate} builds the full prompt, and the AI
 *       (real or offline stub) generates the answer text.</li>
 *   <li>{@code CitationExtractor} pulls structured citations back out of
 *       that answer.</li>
 *   <li>{@code EvaluationHarness} scores how faithful and relevant the
 *       answer actually is.</li>
 *   <li>{@code TraceLogger} records everything about this run — the
 *       question, the prompt, the answer, timing, and every guardrail
 *       decision — as one line in a log file, so it can be reviewed
 *       later.</li>
 * </ol>
 * <p>
 * Why run the guardrails FIRST, before anything expensive: both checks
 * are free (no network calls), so a blocked query costs nothing at all —
 * no wasted document lookup, no wasted (and possibly paid) AI call.
 * <p>
 * Why the retrieval check uses BOTH a threshold AND a margin: a high
 * similarity score alone can be misleading if a completely unrelated
 * chunk happens to score almost as high — that's an ambiguous signal.
 * Requiring the top result to also be clearly ahead of the second-best
 * one catches that case, which checking the score alone would miss.
 */
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

        // Step 3: retrieval guardrail (checks both the absolute score and the margin over the runner-up)
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
