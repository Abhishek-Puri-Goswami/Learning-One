package com.retailco.bankrag.integration.config;

import com.retailco.bankrag.assistant.ExtractiveStubLlmClient;
import com.retailco.bankrag.assistant.LlmClient;
import com.retailco.bankrag.assistant.OpenAiLlmClient;
import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.assistant.TraceLogger;
import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.OpenAiEmbeddingModel;
import com.retailco.bankrag.core.VectorStore;
import com.retailco.bankrag.integration.IntegratedBankingAssistant;
import com.retailco.bankrag.logging.StructuredAuditLogger;
import com.retailco.bankrag.observability.CostEstimator;
import com.retailco.bankrag.observability.MetricsRecorder;
import com.retailco.bankrag.observability.ObservableRagAssistant;
import com.retailco.bankrag.observability.QueryCache;
import com.retailco.bankrag.security.BankingDataStore;
import com.retailco.bankrag.security.BankingToolService;
import com.retailco.bankrag.security.IntentClassifier;
import com.retailco.bankrag.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * This is where every bean the final integrated system needs gets
 * created and wired together, the same "one big configuration class"
 * pattern used earlier in this project — now also creating the
 * audit-logging beans ({@code auditLogPath}, {@code structuredAuditLogger})
 * and passing {@code StructuredAuditLogger} into
 * {@code BankingToolService}'s constructor, which is what turns on the
 * audit trail for every live-data access decision.
 * <p>
 * {@code auditLogPath()} is kept as its own separate bean rather than
 * hardcoding the file path directly inside
 * {@code structuredAuditLogger()} — that way the log file's location is
 * its own independently swappable value; Spring could later be told to
 * supply a different path (say, from configuration) without touching
 * {@code StructuredAuditLogger}'s own code at all.
 */
@Configuration
public class IntegratedAssistantConfig {

    @Value("${bankrag.embedding.dimensions:256}")
    private int embeddingDimensions;

    @Value("${bankrag.assistant.similarity-threshold:0.15}")
    private double similarityThreshold;

    @Value("${bankrag.assistant.min-score-margin:0.012}") // UC4's data-driven corrected default
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

    @Value("${bankrag.security.jwt-secret}")
    private String jwtSecret;

    @Value("${bankrag.security.jwt-expiry-seconds:900}")
    private long jwtExpirySeconds;

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
        return new TraceLogger(Path.of("reports", "integrated-trace-log.jsonl"));
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

    @Bean
    public JwtService jwtService() {
        return new JwtService(jwtSecret, jwtExpirySeconds);
    }

    @Bean
    public BankingDataStore bankingDataStore() {
        return new BankingDataStore();
    }

    @Bean
    public Path auditLogPath() {
        return Path.of("reports", "audit-log.jsonl");
    }

    @Bean
    public StructuredAuditLogger structuredAuditLogger(Path auditLogPath) {
        return new StructuredAuditLogger(auditLogPath);
    }

    @Bean
    public BankingToolService bankingToolService(JwtService jwtService, BankingDataStore bankingDataStore,
                                                   StructuredAuditLogger structuredAuditLogger) {
        return new BankingToolService(jwtService, bankingDataStore, structuredAuditLogger);
    }

    @Bean
    public IntentClassifier intentClassifier() {
        return new IntentClassifier();
    }

    @Bean
    public IntegratedBankingAssistant integratedBankingAssistant(IntentClassifier intentClassifier,
                                                                   ObservableRagAssistant observableRagAssistant,
                                                                   BankingToolService bankingToolService) {
        return new IntegratedBankingAssistant(intentClassifier, observableRagAssistant, bankingToolService);
    }
}
