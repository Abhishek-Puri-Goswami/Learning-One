package com.retailco.bankrag.observability.controller;

import com.retailco.bankrag.observability.dto.IngestRequest;
import com.retailco.bankrag.observability.service.ObservabilityIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A thin controller that loads a corpus into the shared
 * {@code VectorStore} so {@code /ask} has something to search through.
 * All the real work is delegated to {@code ObservabilityIngestionService}.
 */
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
