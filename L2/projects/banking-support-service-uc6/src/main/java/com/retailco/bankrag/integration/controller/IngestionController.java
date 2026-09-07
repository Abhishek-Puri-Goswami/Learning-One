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

/**
 * Loads a folder of text documents into the shared search index (the
 * {@code VectorStore}), so the {@code /ask} endpoint has something to
 * search when answering policy questions. This has to run before
 * {@code /ask} can find anything.
 * <p>
 * Here the request/response types are declared as small records nested
 * right inside this controller, rather than in a separate {@code dto}
 * package like other modules do — a fine, simpler choice for a DTO this
 * small that's only ever used by one controller.
 */
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
