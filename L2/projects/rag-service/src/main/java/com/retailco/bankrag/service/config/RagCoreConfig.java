package com.retailco.bankrag.service.config;

import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
import com.retailco.bankrag.core.OpenAiEmbeddingModel;
import com.retailco.bankrag.core.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// CONCEPT: Spring `@Configuration` class with `@Bean` factory methods --
// the standard place to wire together objects Spring should manage,
// especially when the CHOICE of implementation depends on runtime
// conditions (here: whether an API key is present).
// PURPOSE: This is the ONE place the concrete EmbeddingModel implementation
// is decided for this whole application. No controller or service class
// is ever allowed to write `new OpenAiEmbeddingModel()` or
// `new LocalHashingEmbeddingModel()` itself -- they only ever declare a
// dependency on the `EmbeddingModel` interface, and Spring injects
// whatever this class's @Bean method produced.
//
// HOW @Bean WORKS: Spring calls embeddingModel() once (by default, a
// singleton) during startup, and stores the returned object in its
// "application context." Any other @Component/@Service/@Configuration
// that declares an `EmbeddingModel` constructor parameter (like
// vectorStore() just below) automatically receives that same instance --
// this is Dependency Injection, one of Spring's core ideas.
//
// WHY the real/stub choice belongs HERE and nowhere else: it guarantees
// the "same embedding model for indexing AND querying" rule (see
// EmbeddingModel's own comments) is structurally impossible to violate --
// there's only one bean, used everywhere `EmbeddingModel` is injected.
//
// FLOW: OpenAiEmbeddingModel.isConfigured() checks for OPENAI_API_KEY at
// bean-creation time (application startup) -- real embeddings if present,
// otherwise the offline LocalHashingEmbeddingModel stand-in.
//
// @Value("${bankrag.embedding.dimensions:256}"): pulls a value from
// application.yml/properties (or environment/command-line overrides),
// with 256 as the default if the property isn't set -- Spring's
// externalized configuration mechanism.
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
