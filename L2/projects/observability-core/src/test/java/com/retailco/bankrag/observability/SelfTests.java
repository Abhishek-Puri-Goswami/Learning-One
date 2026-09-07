package com.retailco.bankrag.observability;

import com.retailco.bankrag.assistant.ExtractiveStubLlmClient;
import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.assistant.TraceLogger;
import com.retailco.bankrag.core.Chunk;
import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.ScoredChunk;
import com.retailco.bankrag.core.VectorStore;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Hand-rolled test harness, consistent with every prior use case in this submission. */
public class SelfTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testQueryCacheMissThenHit();
        testQueryCacheNormalizesQuery();
        testQueryCacheEvictsLeastRecentlyUsed();
        testQueryCacheExpiresAfterTtl();
        testMetricsRecorderAggregatesLatencyAndTokens();
        testMetricsRecorderPercentiles();
        testCostEstimatorComputesExpectedCost();
        testCostEstimatorProjectsMonthlyCost();
        testContextOptimizerKeepsHighestScoredChunksFirst();
        testContextOptimizerDropsNothingWhenBudgetIsGenerous();
        testContextOptimizerDropsEverythingWhenBudgetIsZero();
        testThresholdExperimentIdentifiesKnownWrongChunk();
        testThresholdExperimentSweepMonotonicity();
        testObservableRagAssistantCachesRepeatedQuery();
        testObservableRagAssistantRecordsMetricsForBothCacheAndMiss();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testQueryCacheMissThenHit() {
        QueryCache cache = new QueryCache(10, 3600);
        RagAssistant.AssistantResponse dummy = dummyResponse("dummy answer");
        check("cache: miss on first get", cache.get("test query") == null);
        cache.put("test query", dummy);
        check("cache: hit on second get", cache.get("test query") != null);
    }

    private static void testQueryCacheNormalizesQuery() {
        QueryCache cache = new QueryCache(10, 3600);
        cache.put("  What Is My Balance?  ", dummyResponse("x"));
        check("cache: normalized lookup hits (case/whitespace insensitive)",
                cache.get("what is my balance?") != null);
    }

    private static void testQueryCacheEvictsLeastRecentlyUsed() {
        QueryCache cache = new QueryCache(2, 3600);
        cache.put("a", dummyResponse("A"));
        cache.put("b", dummyResponse("B"));
        cache.get("a"); // touch "a" so "b" becomes least-recently-used
        cache.put("c", dummyResponse("C")); // should evict "b", not "a"
        check("cache: LRU eviction keeps recently-used entry", cache.get("a") != null);
        check("cache: LRU eviction removes least-recently-used entry", cache.get("b") == null);
        check("cache: eviction count recorded", cache.stats().evictions() >= 1);
    }

    private static void testQueryCacheExpiresAfterTtl() throws InterruptedException {
        QueryCache cache = new QueryCache(10, 0); // 0-second TTL: expires almost immediately
        cache.put("expiring query", dummyResponse("x"));
        Thread.sleep(1100); // ensure at least 1 full second has elapsed for the epoch-second comparison
        check("cache: entry expired after TTL", cache.get("expiring query") == null);
        check("cache: expiration counted", cache.stats().expirations() >= 1);
    }

    private static void testMetricsRecorderAggregatesLatencyAndTokens() {
        MetricsRecorder recorder = new MetricsRecorder();
        recorder.record(new MetricsRecorder.QueryMetric("q1", false, false, false, 10, 100, 0.01));
        recorder.record(new MetricsRecorder.QueryMetric("q2", false, false, false, 20, 200, 0.02));
        MetricsRecorder.AggregateReport report = recorder.aggregate();
        check("metrics: total queries counted", report.totalQueries() == 2);
        check("metrics: total tokens summed", report.totalTokens() == 300);
        check("metrics: avg latency correct", report.avgLatencyMs() == 15.0);
    }

    private static void testMetricsRecorderPercentiles() {
        MetricsRecorder recorder = new MetricsRecorder();
        for (long latency : List.of(10L, 20L, 30L, 40L, 100L)) {
            recorder.record(new MetricsRecorder.QueryMetric("q", false, false, false, latency, 50, 0.005));
        }
        MetricsRecorder.AggregateReport report = recorder.aggregate();
        check("metrics: p50 is a real median-ish value from the sample", report.p50LatencyMs() == 30L);
        check("metrics: p95 is the highest sample for this small set", report.p95LatencyMs() == 100L);
    }

    private static void testCostEstimatorComputesExpectedCost() {
        CostEstimator estimator = new CostEstimator(new BigDecimal("1.00"), new BigDecimal("2.00")); // $1/1K prompt, $2/1K completion
        BigDecimal cost = estimator.estimateCostUsd(1000, 500); // 1000 prompt tokens + 500 completion tokens
        check("cost: computed cost matches manual calculation ($1.00 + $1.00 = $2.00)",
                cost.compareTo(new BigDecimal("2.000000")) == 0);
    }

    private static void testCostEstimatorProjectsMonthlyCost() {
        CostEstimator estimator = CostEstimator.illustrativeDefault();
        BigDecimal monthly = estimator.projectMonthlyCost(new BigDecimal("0.05"), 100); // $0.05/query, 100 queries/day
        check("cost: monthly projection = avgCost * queriesPerDay * 30",
                monthly.compareTo(new BigDecimal("150.00")) == 0);
    }

    private static void testContextOptimizerKeepsHighestScoredChunksFirst() {
        Chunk c1 = new Chunk("doc#0", "doc", 0, "one two three four five"); // 5 tokens
        Chunk c2 = new Chunk("doc#1", "doc", 1, "six seven eight nine ten"); // 5 tokens
        List<ScoredChunk> chunks = List.of(new ScoredChunk(c1, 0.9), new ScoredChunk(c2, 0.5));
        ContextOptimizer optimizer = new ContextOptimizer(5); // only room for one chunk
        ContextOptimizer.OptimizationResult result = optimizer.optimize(chunks);
        check("context: keeps only the higher-scored chunk under a tight budget",
                result.keptChunks().size() == 1 && result.keptChunks().get(0).chunk().id().equals("doc#0"));
        check("context: dropped chunk counted", result.droppedChunkCount() == 1);
    }

    private static void testContextOptimizerDropsNothingWhenBudgetIsGenerous() {
        Chunk c1 = new Chunk("doc#0", "doc", 0, "one two three");
        List<ScoredChunk> chunks = List.of(new ScoredChunk(c1, 0.9));
        ContextOptimizer optimizer = new ContextOptimizer(10000);
        ContextOptimizer.OptimizationResult result = optimizer.optimize(chunks);
        check("context: generous budget keeps everything", result.droppedChunkCount() == 0);
    }

    private static void testContextOptimizerDropsEverythingWhenBudgetIsZero() {
        Chunk c1 = new Chunk("doc#0", "doc", 0, "one two three");
        List<ScoredChunk> chunks = List.of(new ScoredChunk(c1, 0.9));
        ContextOptimizer optimizer = new ContextOptimizer(0);
        ContextOptimizer.OptimizationResult result = optimizer.optimize(chunks);
        check("context: zero budget drops everything", result.keptChunks().isEmpty() && result.droppedChunkCount() == 1);
    }

    private static void testThresholdExperimentIdentifiesKnownWrongChunk() throws IOException {
        VectorStore vectorStore = buildDemoVectorStore();
        ThresholdTuningExperiment experiment = new ThresholdTuningExperiment(vectorStore, 0.6, 0.4, 0.15);
        List<ThresholdTuningExperiment.LabeledQuery> queries = List.of(
                new ThresholdTuningExperiment.LabeledQuery(
                        "What is the interest rate range for a home loan?", "loan_processing_policy.txt#0"));
        List<ThresholdTuningExperiment.QueryOutcome> outcomes = experiment.runQueries(queries);
        check("threshold-experiment: reproduces UC2's known wrong-top-chunk finding",
                !outcomes.get(0).topChunkCorrect());
    }

    private static void testThresholdExperimentSweepMonotonicity() throws IOException {
        VectorStore vectorStore = buildDemoVectorStore();
        ThresholdTuningExperiment experiment = new ThresholdTuningExperiment(vectorStore, 0.6, 0.4, 0.15);
        List<ThresholdTuningExperiment.QueryOutcome> outcomes = experiment.runQueries(ThresholdTuningExperiment.defaultLabeledQueries());
        List<ThresholdTuningExperiment.ThresholdResult> results = experiment.sweep(outcomes, new double[]{0.0, 0.05, 0.5});
        // Raising the margin threshold can only ever move queries from
        // "answered" to "blocked" (correctly or incorrectly), never the
        // reverse -- so correctlyAnswered + incorrectlyAnswered should be
        // non-increasing as the threshold rises.
        int answeredAtLow = results.get(0).correctlyAnswered() + results.get(0).incorrectlyAnswered();
        int answeredAtHigh = results.get(2).correctlyAnswered() + results.get(2).incorrectlyAnswered();
        check("threshold-experiment: higher threshold answers no more queries than a lower one",
                answeredAtHigh <= answeredAtLow);
    }

    private static void testObservableRagAssistantCachesRepeatedQuery() throws IOException {
        ObservableRagAssistant observable = buildObservableAssistant();
        ObservableRagAssistant.ObservedResponse first = observable.ask("What documents are required for KYC verification?");
        ObservableRagAssistant.ObservedResponse second = observable.ask("What documents are required for KYC verification?");
        check("observable: first call is a cache miss", !first.servedFromCache());
        check("observable: repeated call is a cache hit", second.servedFromCache());
        check("observable: cached response has identical answer text",
                first.response().answer().equals(second.response().answer()));
    }

    private static void testObservableRagAssistantRecordsMetricsForBothCacheAndMiss() throws IOException {
        ObservableRagAssistant observable = buildObservableAssistant();
        observable.ask("What documents are required for KYC verification?");
        observable.ask("What documents are required for KYC verification?"); // cache hit
        MetricsRecorder.AggregateReport report = observable.metricsReport();
        check("observable: metrics recorded for both calls", report.totalQueries() == 2);
        check("observable: one cache hit recorded", report.cacheHits() == 1);
    }

    private static VectorStore buildDemoVectorStore() throws IOException {
        EmbeddingModel embeddingModel = new LocalHashingEmbeddingModel(256);
        VectorStore vectorStore = new VectorStore(embeddingModel);
        com.retailco.bankrag.core.Chunker chunker = new com.retailco.bankrag.core.Chunker(
                com.retailco.bankrag.core.ChunkingConfig.defaultConfig());
        com.retailco.bankrag.core.DocumentLoader loader = new com.retailco.bankrag.core.DocumentLoader();
        for (var doc : loader.loadTextDirectory(Path.of("corpus"))) {
            for (Chunk c : chunker.chunk(doc.id(), doc.text())) {
                vectorStore.index(c);
            }
        }
        return vectorStore;
    }

    private static ObservableRagAssistant buildObservableAssistant() throws IOException {
        VectorStore vectorStore = buildDemoVectorStore();
        Path tmpTrace = Files.createTempFile("obs-trace-test", ".jsonl");
        RagAssistant ragAssistant = new RagAssistant(vectorStore, new ExtractiveStubLlmClient(),
                new TraceLogger(tmpTrace), 0.6, 0.4, 0.15, 0.03, 3);
        return new ObservableRagAssistant(ragAssistant, new QueryCache(10, 3600),
                new MetricsRecorder(), CostEstimator.illustrativeDefault());
    }

    private static RagAssistant.AssistantResponse dummyResponse(String answer) {
        return new RagAssistant.AssistantResponse("q", answer, List.of(), false, null, false,
                new com.retailco.bankrag.assistant.EvaluationHarness.EvaluationResult(1.0, 0.5, 5, 10));
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name);
            failed++;
        }
    }
}
