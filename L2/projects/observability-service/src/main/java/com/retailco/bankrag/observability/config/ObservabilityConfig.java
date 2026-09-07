package com.retailco.bankrag.observability.config;

import com.retailco.bankrag.assistant.ExtractiveStubLlmClient;
import com.retailco.bankrag.assistant.LlmClient;
import com.retailco.bankrag.assistant.OpenAiLlmClient;
import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.assistant.TraceLogger;
import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.OpenAiEmbeddingModel;
import com.retailco.bankrag.core.VectorStore;
import com.retailco.bankrag.observability.CostEstimator;
import com.retailco.bankrag.observability.MetricsRecorder;
import com.retailco.bankrag.observability.ObservableRagAssistant;
import com.retailco.bankrag.observability.QueryCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * Same central-bean-wiring philosophy as every prior use case's config
 * class: every concrete implementation choice (embedding model, LLM
 * client, cache sizing, cost rate) lives here, once.
 */
@Configuration
public class ObservabilityConfig {

    @Value("${bankrag.embedding.dimensions:256}")
    private int embeddingDimensions;

    @Value("${bankrag.assistant.similarity-threshold:0.15}")
    private double similarityThreshold;

    @Value("${bankrag.assistant.min-score-margin:0.012}") // lowered from UC2's 0.03 per this use case's real threshold-tuning experiment
    private double minScoreMargin;

    @Value("${bankrag.assistant.semantic-weight:0.6}")
    private double semanticWeight;

    @Value("${bankrag.assistant.keyword-weight:0.4}")
    private double keywordWeight;

    @Value("${bankrag.assistant.top-k:3}")
    private int topK;

    @Value("${bankrag.cache.max-entries:500}")
    private int cacheMaxEntries;

    @Value("${bankrag.cache.ttl-seconds:3600}")
    private long cacheTtlSeconds;

    @Value("${bankrag.cost.per-1k-prompt-tokens-usd:0.15}")
    private String costPer1kPromptTokens;

    @Value("${bankrag.cost.per-1k-completion-tokens-usd:0.60}")
    private String costPer1kCompletionTokens;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.isConfigured()
                ? new OpenAiEmbeddingModel()
                : new LocalHashingEmbeddingModel(embeddingDimensions);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new VectorStore(embeddingModel);
    }

    @Bean
    public LlmClient llmClient() {
        return OpenAiLlmClient.isConfigured()
                ? new OpenAiLlmClient()
                : new ExtractiveStubLlmClient();
    }

    @Bean
    public TraceLogger traceLogger() {
        return new TraceLogger(Path.of("reports", "observability-trace-log.jsonl"));
    }

    @Bean
    public RagAssistant ragAssistant(VectorStore vectorStore, LlmClient llmClient, TraceLogger traceLogger) {
        return new RagAssistant(vectorStore, llmClient, traceLogger,
                semanticWeight, keywordWeight, similarityThreshold, minScoreMargin, topK);
    }

    @Bean
    public QueryCache queryCache() {
        return new QueryCache(cacheMaxEntries, cacheTtlSeconds);
    }

    @Bean
    public MetricsRecorder metricsRecorder() {
        return new MetricsRecorder();
    }

    @Bean
    public CostEstimator costEstimator() {
        return new CostEstimator(new BigDecimal(costPer1kPromptTokens), new BigDecimal(costPer1kCompletionTokens));
    }

    @Bean
    public ObservableRagAssistant observableRagAssistant(RagAssistant ragAssistant, QueryCache queryCache,
                                                           MetricsRecorder metricsRecorder, CostEstimator costEstimator) {
        return new ObservableRagAssistant(ragAssistant, queryCache, metricsRecorder, costEstimator);
    }
}
