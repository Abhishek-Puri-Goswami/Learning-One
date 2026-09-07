package com.retailco.bankrag.service.controller;

import com.retailco.bankrag.service.dto.IngestRequest;
import com.retailco.bankrag.service.dto.IngestResponse;
import com.retailco.bankrag.service.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Step 1 of the "Foundation & Core Retrieval" REST surface: document
 * ingestion. See design/embedding-generation-module.md's Module Boundaries
 * diagram -- IngestionController -> DocumentLoader -> Chunker ->
 * EmbeddingModel -> VectorStore.
 */
@RestController
@RequestMapping("/api/v1/rag")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest(@Valid @RequestBody IngestRequest request) {
        return ResponseEntity.ok(ingestionService.ingest(request));
    }
}
