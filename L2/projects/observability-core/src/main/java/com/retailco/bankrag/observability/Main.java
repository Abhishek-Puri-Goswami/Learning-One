package com.retailco.bankrag.observability;

import com.retailco.bankrag.assistant.ExtractiveStubLlmClient;
import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.assistant.TraceLogger;
import com.retailco.bankrag.core.Chunk;
import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ChunkingConfig;
import com.retailco.bankrag.core.DocumentLoader;
import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.HybridSearcher;
import com.retailco.bankrag.core.KeywordSearcher;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.OpenAiEmbeddingModel;
import com.retailco.bankrag.assistant.LlmClient;
import com.retailco.bankrag.assistant.OpenAiLlmClient;
import com.retailco.bankrag.core.ScoredChunk;
import com.retailco.bankrag.core.VectorStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CLI demo for L2 UC4 Intelligence Maturity & Optimization. Three real,
 * independently-run experiments:
 *   1. ObservableRagAssistant: caching + metrics + cost, over a query set
 *      that deliberately repeats some queries to produce real cache hits.
 *   2. ContextOptimizer: token-budget trimming over real retrieved chunks.
 *   3. ThresholdTuningExperiment: the margin-threshold sweep answering
 *      L2/UC2's evaluation-summary "Tuning Tension" recommendation.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String corpusDir = args.length > 0 ? args[0] : "corpus";

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.isConfigured()
                ? new OpenAiEmbeddingModel()
                : new LocalHashingEmbeddingModel(256);
        LlmClient llmClient = OpenAiLlmClient.isConfigured()
                ? new OpenAiLlmClient()
                : new ExtractiveStubLlmClient();
        System.out.println(OpenAiEmbeddingModel.isConfigured()
                ? "OPENAI_API_KEY detected -- using real OpenAI embeddings."
                : "OPENAI_API_KEY not set -- using offline LocalHashingEmbeddingModel stand-in.");
        System.out.println(OpenAiLlmClient.isConfigured()
                ? "OPENAI_API_KEY detected -- using real OpenAI chat completions."
                : "OPENAI_API_KEY not set -- using offline ExtractiveStubLlmClient stand-in.");
        VectorStore vectorStore = new VectorStore(embeddingModel);
        ChunkingConfig config = ChunkingConfig.defaultConfig();
        Chunker chunker = new Chunker(config);
        DocumentLoader loader = new DocumentLoader();
        List<DocumentLoader.SourceDocument> docs = loader.loadTextDirectory(Path.of(corpusDir));
        for (DocumentLoader.SourceDocument doc : docs) {
            for (Chunk c : chunker.chunk(doc.id(), doc.text())) {
                vectorStore.index(c);
            }
        }
        System.out.println("Loaded " + docs.size() + " documents into VectorStore.");
        System.out.println("=".repeat(100));

        Path reportsDir = Path.of("reports");
        Files.createDirectories(reportsDir);
        Path traceLogPath = reportsDir.resolve("observability-trace-log.jsonl");
        Files.deleteIfExists(traceLogPath);

        System.out.println("EXPERIMENT 1: Caching + Metrics + Cost (ObservableRagAssistant)");
        System.out.println("=".repeat(100));

        RagAssistant ragAssistant = new RagAssistant(vectorStore, llmClient,
                new TraceLogger(traceLogPath), 0.6, 0.4, 0.15, 0.03, 3);
        QueryCache cache = new QueryCache(50, 3600);
        MetricsRecorder metricsRecorder = new MetricsRecorder();
        CostEstimator costEstimator = CostEstimator.illustrativeDefault();
        ObservableRagAssistant observable = new ObservableRagAssistant(ragAssistant, cache, metricsRecorder, costEstimator);

        // Deliberately repeats 2 queries (indices 1 and 3 repeat 0 and 2) to
        // produce real, measurable cache hits -- a realistic pattern for a
        // small number of FAQ-style questions asked by many different
        // customers.
        List<String> queryTraffic = List.of(
                "How do I file a complaint and what is the escalation process?",
                "What is the minimum monthly income required for a personal loan?",
                "How do I file a complaint and what is the escalation process?", // repeat -> cache hit
                "What documents are required for KYC verification?",
                "What is the minimum monthly income required for a personal loan?", // repeat -> cache hit
                "Should I invest my savings in mutual funds right now?", // blocked, still cached
                "Should I invest my savings in mutual funds right now?" // repeat of blocked query -> cache hit
        );

        for (String q : queryTraffic) {
            ObservableRagAssistant.ObservedResponse observed = observable.ask(q);
            System.out.printf("[%s] \"%s\"%n", observed.servedFromCache() ? "CACHE HIT" : "MISS", q);
        }

        System.out.println();
        System.out.println("Cache stats: " + observable.cacheStats());
        System.out.println("Metrics report: " + observable.metricsReport());

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("EXPERIMENT 2: Context optimization (token-budget trimming)");
        System.out.println("=".repeat(100));

        HybridSearcher searcher = new HybridSearcher(vectorStore, new KeywordSearcher(), 0.6, 0.4);
        List<ScoredChunk> retrieved = searcher.search("What is the minimum monthly income required for a personal loan?", 5, 0.0);
        int totalTokensUnoptimized = retrieved.stream()
                .mapToInt(sc -> Chunker.tokenize(sc.chunk().text()).length).sum();
        System.out.println("Retrieved " + retrieved.size() + " chunks, " + totalTokensUnoptimized + " tokens total (unoptimized).");

        for (int budget : new int[]{600, 400, 200}) {
            ContextOptimizer optimizer = new ContextOptimizer(budget);
            ContextOptimizer.OptimizationResult result = optimizer.optimize(retrieved);
            System.out.printf("Budget=%d tokens -> kept %d/%d chunks (%d tokens), dropped %d chunks (%d tokens)%n",
                    budget, result.keptChunks().size(), retrieved.size(), result.keptTokens(),
                    result.droppedChunkCount(), result.droppedTokens());
        }

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("EXPERIMENT 3: Similarity/margin threshold tuning sweep");
        System.out.println("=".repeat(100));

        ThresholdTuningExperiment experiment = new ThresholdTuningExperiment(vectorStore, 0.6, 0.4, 0.15);
        List<ThresholdTuningExperiment.LabeledQuery> labeled = ThresholdTuningExperiment.defaultLabeledQueries();
        List<ThresholdTuningExperiment.QueryOutcome> outcomes = experiment.runQueries(labeled);

        System.out.println("Per-query outcomes (real retrieval results):");
        for (ThresholdTuningExperiment.QueryOutcome o : outcomes) {
            System.out.printf("  \"%s\"%n    expected=%s actual=%s correct=%s topScore=%.4f margin=%.4f%n",
                    o.query(), o.expectedChunkId(), o.actualTopChunkId(), o.topChunkCorrect(), o.topScore(), o.margin());
        }

        System.out.println();
        System.out.println("Threshold sweep (candidate margin thresholds):");
        double[] candidates = {0.0, 0.005, 0.01, 0.015, 0.02, 0.03, 0.05, 0.08};
        List<ThresholdTuningExperiment.ThresholdResult> results = experiment.sweep(outcomes, candidates);
        System.out.printf("%-10s %-18s %-18s %-18s %-18s%n", "margin", "correctlyAnswered", "correctlyBlocked", "incorrectlyBlocked", "incorrectlyAnswered");
        for (ThresholdTuningExperiment.ThresholdResult r : results) {
            System.out.printf("%-10.3f %-18d %-18d %-18d %-18d%n",
                    r.threshold(), r.correctlyAnswered(), r.correctlyBlocked(), r.incorrectlyBlocked(), r.incorrectlyAnswered());
        }

        System.out.println();
        System.out.println("All experiments completed. Trace log: " + traceLogPath.toAbsolutePath());
    }
}
