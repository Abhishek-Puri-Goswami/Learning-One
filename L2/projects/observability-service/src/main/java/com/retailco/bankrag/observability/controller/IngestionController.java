package com.retailco.bankrag.observability.controller;

import com.retailco.bankrag.observability.dto.IngestRequest;
import com.retailco.bankrag.observability.service.ObservabilityIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CONCEPT/PURPOSE: same thin Controller-layer ingestion pattern as the
// other modules (loads a corpus into the shared VectorStore so /ask has
// something to retrieve). All real work is in ObservabilityIngestionService.
@RestController
@RequestMapping("/api/v1/observability")
public class IngestionController {

    private final ObservabilityIngestionService ingestionService;

    public IngestionController(ObservabilityIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<ObservabilityIngestionService.IngestResult> ingest(@Valid @RequestBody IngestRequest request) {
        return ResponseEntity.ok(ingestionService.ingest(request));
    }
}
