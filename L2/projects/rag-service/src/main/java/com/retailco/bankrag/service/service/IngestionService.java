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
 * Contains the business logic for ingestion, sitting between the
 * Controller and the underlying domain classes ({@code DocumentLoader},
 * {@code Chunker}, {@code VectorStore}). It orchestrates the whole
 * pipeline for one request: load documents from disk, chunk each one, and
 * index every chunk into the shared vector store.
 * <p>
 * Keeping this logic in a {@code @Service} instead of directly in the
 * controller means this class can be tested on its own, without needing
 * to spin up any web server — and it keeps the controller focused purely
 * on handling HTTP requests and responses.
 * <p>
 * One thing worth understanding: {@code vectorStore} is a single,
 * shared object (a Spring-managed "bean" — see {@code RagCoreConfig}) and
 * {@code index()} adds to it in place, rather than replacing it. That
 * means repeated calls to {@code ingest()} keep ADDING to a growing set
 * of indexed documents, rather than starting over each time — matching
 * how a real ingestion pipeline runs continuously as new documents
 * arrive, not as a single one-time batch job.
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
            // The caller explicitly asked for a custom chunk size/overlap.
            // Because ChunkingConfig takes these as constructor
            // parameters rather than fixed constants, a future document
            // type can be ingested with different sizing with no code
            // change needed at all.
            return new ChunkingConfig(request.chunkSizeTokens(), request.overlapTokens());
        }
        return ChunkingConfig.defaultConfig();
    }
}
