package com.retailco.bankrag.service.config;

import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.OpenAiEmbeddingModel;
import com.retailco.bankrag.core.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is the ONE place that decides which concrete
 * {@code EmbeddingModel} implementation the whole application uses — the
 * real OpenAI one, or the offline stand-in. No controller or service
 * class is ever allowed to create these directly; they only ever declare
 * a dependency on the {@code EmbeddingModel} interface, and Spring
 * automatically hands them whatever this class's {@code @Bean} method
 * produced.
 * <p>
 * How {@code @Bean} works: Spring calls {@code embeddingModel()} once, at
 * startup, and remembers the object it returns. Any other class that
 * asks for an {@code EmbeddingModel} (like {@code vectorStore()} right
 * below) automatically receives that exact same instance — this
 * automatic wiring-together of objects is called Dependency Injection,
 * one of Spring's core ideas.
 * <p>
 * Why this decision belongs HERE and nowhere else: it guarantees the
 * "same embedding model for storing AND searching" rule can never
 * accidentally be broken — there's only one bean, used everywhere an
 * {@code EmbeddingModel} is needed.
 * <p>
 * The {@code @Value} annotation below pulls a setting from configuration
 * (like {@code application.yml}), falling back to 256 if it isn't set —
 * Spring's standard way of making settings configurable per environment.
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
