package com.retailco.bankrag.assistant.config;

import com.retailco.bankrag.assistant.ExtractiveStubLlmClient;
import com.retailco.bankrag.assistant.LlmClient;
import com.retailco.bankrag.assistant.OpenAiLlmClient;
import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.assistant.TraceLogger;
import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.OpenAiEmbeddingModel;
import com.retailco.bankrag.core.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

// CONCEPT: Spring `@Configuration` class -- the central wiring point for
// this whole module's object graph (same philosophy as rag-service's
// RagCoreConfig, extended here with an LlmClient and the full RagAssistant).
// PURPOSE: Decides, ONCE, which concrete EmbeddingModel and LlmClient
// implementations the entire application uses (real OpenAI-backed vs.
// offline stand-ins), based on whether OPENAI_API_KEY is present -- see
// each *.isConfigured() check below. No controller or other class is
// ever allowed to construct these directly.
// HOW THE BEANS CHAIN TOGETHER (dependency injection, see the method
// signatures below): embeddingModel() has no dependencies -> vectorStore()
// depends on embeddingModel -> ragAssistant() depends on vectorStore,
// llmClient, AND traceLogger. Spring resolves this whole dependency graph
// automatically at startup, in the correct order, just from each method's
// parameter types -- you never manually call one @Bean method from another.
// WHY the numeric/string @Value fields matter: every tunable knob for the
// RAG pipeline (thresholds, weights, topK, trace log path) is externalized
// to application.yml/environment variables here, rather than hardcoded
// inside RagAssistant itself -- so behavior can be tuned per environment
// without touching business logic code.
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
        return new TraceLogger(Path.of(traceLogPath));
    }

    @Bean
    public RagAssistant ragAssistant(VectorStore vectorStore, LlmClient llmClient, TraceLogger traceLogger) {
        return new RagAssistant(vectorStore, llmClient, traceLogger,
                semanticWeight, keywordWeight, similarityThreshold, minScoreMargin, topK);
    }
}
