package com.retailco.bankrag.integration.controller;

import com.retailco.bankrag.core.Chunk;
import com.retailco.bankrag.core.Chunker;
import com.retailco.bankrag.core.ChunkingConfig;
import com.retailco.bankrag.core.DocumentLoader;
import com.retailco.bankrag.core.VectorStore;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

// CONCEPT/PURPOSE: same ingestion-endpoint pattern as the other modules'
// IngestionController (loads a corpus into the shared VectorStore before
// /ask can retrieve anything). IMPORTANT: here the request/response DTOs
// are declared as small nested records INSIDE the controller itself,
// rather than in a separate dto/ package -- a valid, simpler alternative
// when a DTO is trivial and used by exactly one controller, though the
// other modules in this submission use a separate dto/ package instead.
@RestController
@RequestMapping("/api/v1/support")
public class IngestionController {

    public record IngestRequest(@NotBlank String corpusDirectory) {
    }

    public record IngestResult(int documentsLoaded, int chunksIndexed) {
    }

    private final VectorStore vectorStore;
    private final DocumentLoader documentLoader = new DocumentLoader();

    public IngestionController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResult> ingest(@RequestBody IngestRequest request) {
        Chunker chunker = new Chunker(ChunkingConfig.defaultConfig());
        var docs = load(request.corpusDirectory());
        int chunksIndexed = 0;
        for (var doc : docs) {
            for (Chunk c : chunker.chunk(doc.id(), doc.text())) {
                vectorStore.index(c);
                chunksIndexed++;
            }
        }
        return ResponseEntity.ok(new IngestResult(docs.size(), chunksIndexed));
    }

    private java.util.List<DocumentLoader.SourceDocument> load(String corpusDirectory) {
        try {
            return documentLoader.loadTextDirectory(Path.of(corpusDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load corpus directory: " + corpusDirectory, e);
        }
    }
}
