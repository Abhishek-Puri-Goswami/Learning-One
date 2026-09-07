package com.retailco.bankrag.observability.service;

import com.retailco.bankrag.core.Chunk;
import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ChunkingConfig;
import com.retailco.bankrag.core.DocumentLoader;
import com.retailco.bankrag.core.VectorStore;
import com.retailco.bankrag.observability.dto.IngestRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

// CONCEPT/PURPOSE: same Service-layer ingestion pipeline
// (DocumentLoader -> Chunker -> VectorStore) as rag-service's
// IngestionService and rag-assistant-service's AssistantIngestionService --
// duplicated here rather than shared because each Spring module in this
// submission copies rag-core's classes as source instead of depending on
// a sibling module's jar (see rag-core's own comments for that reasoning).
@Service
public class ObservabilityIngestionService {

    private final DocumentLoader documentLoader = new DocumentLoader();
    private final VectorStore vectorStore;

    public ObservabilityIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public record IngestResult(int documentsLoaded, int chunksIndexed) {
    }

    public IngestResult ingest(IngestRequest request) {
        Chunker chunker = new Chunker(ChunkingConfig.defaultConfig());
        var documents = load(request.corpusDirectory());
        int chunksIndexed = 0;
        for (var doc : documents) {
            for (Chunk chunk : chunker.chunk(doc.id(), doc.text())) {
                vectorStore.index(chunk);
                chunksIndexed++;
            }
        }
        return new IngestResult(documents.size(), chunksIndexed);
    }

    private java.util.List<DocumentLoader.SourceDocument> load(String corpusDirectory) {
        try {
            return documentLoader.loadTextDirectory(Path.of(corpusDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load corpus directory: " + corpusDirectory, e);
        }
    }
}
