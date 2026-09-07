package com.retailco.bankrag.service.service;

import com.retailco.bankrag.core.Chunk;
import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ChunkingConfig;
import com.retailco.bankrag.core.DocumentLoader;
import com.retailco.bankrag.core.VectorStore;
import com.retailco.bankrag.service.dto.IngestRequest;
import com.retailco.bankrag.service.dto.IngestResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Wraps rag-core's DocumentLoader + Chunker + VectorStore pipeline
 * (IngestionController -> DocumentLoader -> Chunker -> EmbeddingModel ->
 * VectorStore, per design/embedding-generation-module.md's "Module
 * Boundaries" diagram) behind a REST-callable service.
 *
 * The shared VectorStore bean is intentionally mutated in place (index() is
 * additive) so repeated ingest calls accumulate a growing corpus, matching
 * how a real ingestion pipeline is expected to run incrementally as new
 * policy documents are added -- not as a one-shot batch job.
 */
@Service
public class IngestionService {

    private final DocumentLoader documentLoader = new DocumentLoader();
    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public IngestResponse ingest(IngestRequest request) {
        ChunkingConfig config = resolveConfig(request);
        Chunker chunker = new Chunker(config);

        List<DocumentLoader.SourceDocument> documents;
        try {
            documents = documentLoader.loadTextDirectory(Path.of(request.corpusDirectory()));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to load corpus directory: " + request.corpusDirectory(), e);
        }

        int chunksIndexed = 0;
        for (DocumentLoader.SourceDocument doc : documents) {
            List<Chunk> chunks = chunker.chunk(doc.id(), doc.text());
            for (Chunk chunk : chunks) {
                vectorStore.index(chunk);
                chunksIndexed++;
            }
        }

        return new IngestResponse(documents.size(), chunksIndexed,
                config.chunkSizeTokens(), config.overlapTokens());
    }

    private ChunkingConfig resolveConfig(IngestRequest request) {
        if (request.chunkSizeTokens() != null && request.overlapTokens() != null) {
            // Explicit override -- see design/chunking-configuration.md's note that
            // ChunkingConfig is a constructor parameter, not a hardcoded constant,
            // precisely so a future document type can be ingested with different
            // sizing without a code change.
            return new ChunkingConfig(request.chunkSizeTokens(), request.overlapTokens());
        }
        return ChunkingConfig.defaultConfig();
    }
}
