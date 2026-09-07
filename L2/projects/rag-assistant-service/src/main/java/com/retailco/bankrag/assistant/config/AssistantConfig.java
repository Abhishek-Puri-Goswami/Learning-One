package com.retailco.bankrag.assistant.config;

import com.retailco.bankrag.assistant.ExtractiveStubLlmClient;
import com.retailco.bankrag.assistant.LlmClient;
import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.assistant.TraceLogger;
import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Central bean wiring, same philosophy as L2/UC1's RagCoreConfig: the
 * concrete LlmClient and EmbeddingModel implementations are chosen ONCE,
 * here, by profile -- never instantiated ad hoc inside a controller.
 *
 * Only the "local" profile is implemented today: LocalHashingEmbeddingModel
 * (UC1's stand-in) and ExtractiveStubLlmClient (UC2's stand-in) -- both
 * disclosed as non-production in their own Javadoc. A "prod" profile would
 * swap in a real embedding provider AND a real LlmClient implementation
 * (e.g. an OpenAiLlmClient calling the Chat Completions API) -- the
 * LlmClient interface is exactly what makes that swap mechanical, per
 * prompts/prompt-template-design.md.
 */
@Configuration
public class AssistantConfig {

    @Value("${bankrag.embedding.dimensions:256}")
    private int embeddingDimensions;

    @Value("${bankrag.assistant.similarity-threshold:0.15}")
    private double similarityThreshold;

    @Value("${bankrag.assistant.min-score-margin:0.03}")
    private double minScoreMargin;

    @Value("${bankrag.assistant.semantic-weight:0.6}")
    private double semanticWeight;

    @Value("${bankrag.assistant.keyword-weight:0.4}")
    private double keywordWeight;

    @Value("${bankrag.assistant.top-k:3}")
    private int topK;

    @Value("${bankrag.assistant.trace-log-path:reports/langsmith-style-trace-log.jsonl}")
    private String traceLogPath;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new LocalHashingEmbeddingModel(embeddingDimensions);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new VectorStore(embeddingModel);
    }

    @Bean
    public LlmClient llmClient() {
        return new ExtractiveStubLlmClient();
    }

    @Bean
    public TraceLogger traceLogger() {
        return new TraceLogger(Path.of(traceLogPath));
    }

    @Bean
    public RagAssistant ragAssistant(VectorStore vectorStore, LlmClient llmClient, TraceLogger traceLogger) {
        return new RagAssistant(vectorStore, llmClient, traceLogger,
                semanticWeight, keywordWeight, similarityThreshold, minScoreMargin, topK);
    }
}
