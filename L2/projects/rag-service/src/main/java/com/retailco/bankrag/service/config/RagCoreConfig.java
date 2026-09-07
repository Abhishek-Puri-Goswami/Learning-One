package com.retailco.bankrag.service.config;

import com.retailco.bankrag.core.EmbeddingModel;
import com.retailco.bankrag.core.LocalHashingEmbeddingModel;
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
 * Today only the "local" profile is implemented (LocalHashingEmbeddingModel
 * -- see that class's Javadoc for why: no reachable embedding API or model
 * download in this sandbox). A "prod" profile bean
 * (OpenAiEmbeddingModel/AzureOpenAiEmbeddingModel/etc., selected via
 * spring.profiles.active=prod) is the documented next step in
 * design/embedding-generation-module.md's swap-in table and is intentionally
 * left as a follow-on so this use case's scope stays "foundation," not
 * "production embedding integration" (that belongs to later L2 use cases).
 */
@Configuration
public class RagCoreConfig {

    @Value("${bankrag.embedding.dimensions:256}")
    private int embeddingDimensions;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new LocalHashingEmbeddingModel(embeddingDimensions);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new VectorStore(embeddingModel);
    }
}
