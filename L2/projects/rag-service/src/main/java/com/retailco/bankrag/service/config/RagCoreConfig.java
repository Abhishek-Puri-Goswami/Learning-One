package com.retailco.bankrag.service.config;

import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.OpenAiEmbeddingModel;
import com.retailco.bankrag.core.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central bean wiring for rag-core's components -- per
 * design/embedding-generation-module.md's "Module Boundaries" note: the
 * concrete EmbeddingModel implementation is chosen ONCE, here, by profile,
 * never instantiated ad hoc inside a controller or service class. This is
 * what keeps the "same model for indexing and querying" rule (L2 reference
 * guide section 3.1) structurally enforced rather than merely a coding
 * convention.
 *
 * When an {@code OPENAI_API_KEY} environment variable is present,
 * {@link OpenAiEmbeddingModel} is used; otherwise this falls back to
 * {@link LocalHashingEmbeddingModel} for fully offline operation.
 */
@Configuration
public class RagCoreConfig {

    @Value("${bankrag.embedding.dimensions:256}")
    private int embeddingDimensions;

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
}
