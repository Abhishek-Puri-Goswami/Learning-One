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

// CONCEPT: Service layer -- contains the business logic between the
// Controller and the underlying domain classes (DocumentLoader, Chunker,
// VectorStore).
// PURPOSE: Orchestrates the full ingestion pipeline for one request: load
// documents from disk, chunk each one, embed and index every chunk into
// the shared VectorStore.
// FLOW: Controller -> Service -> (DocumentLoader, Chunker, VectorStore)
// WHY keep this logic in a @Service rather than in the controller:
// separating it lets this class be unit-tested without spinning up any
// HTTP infrastructure, and keeps IngestionController focused purely on
// request/response plumbing.
// IMPORTANT: `vectorStore` is injected as a Spring-managed singleton bean
// (see RagCoreConfig) and mutated IN PLACE by index() -- so repeated calls
// to ingest() ACCUMULATE into a growing corpus rather than replacing it
// each time. This matches how a real ingestion pipeline runs incrementally
// as new documents arrive, not as a single one-shot batch job.
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
