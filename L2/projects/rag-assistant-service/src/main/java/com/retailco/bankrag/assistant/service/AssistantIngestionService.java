package com.retailco.bankrag.assistant.service;

import com.retailco.bankrag.assistant.dto.IngestRequest;
import com.retailco.bankrag.assistant.dto.IngestResponse;
import com.retailco.bankrag.core.Chunk;
import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ChunkingConfig;
import com.retailco.bankrag.core.DocumentLoader;
import com.retailco.bankrag.core.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads the policy corpus into the shared VectorStore bean before /ask can
 * answer anything -- same DocumentLoader -> Chunker -> VectorStore pipeline
 * as L2/UC1's IngestionService, reused here because the retrieval
 * foundation doesn't change between UC1 and UC2, only what's built on top
 * of it does.
 */
@Service
public class AssistantIngestionService {

    private final DocumentLoader documentLoader = new DocumentLoader();
    private final VectorStore vectorStore;

    public AssistantIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public IngestResponse ingest(IngestRequest request) {
        Chunker chunker = new Chunker(ChunkingConfig.defaultConfig());
        List<DocumentLoader.SourceDocument> documents;
        try {
            documents = documentLoader.loadTextDirectory(Path.of(request.corpusDirectory()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load corpus directory: " + request.corpusDirectory(), e);
        }

        int chunksIndexed = 0;
        for (DocumentLoader.SourceDocument doc : documents) {
            List<Chunk> chunks = chunker.chunk(doc.id(), doc.text());
            for (Chunk chunk : chunks) {
                vectorStore.index(chunk);
                chunksIndexed++;
            }
        }
        return new IngestResponse(documents.size(), chunksIndexed);
    }
}
