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

// CONCEPT: Controller layer (Spring MVC's `@RestController`) -- the thin
// HTTP-facing layer that translates a REST request into a service call.
// PURPOSE: Exposes POST /api/v1/rag/ingest, which loads a directory of
// documents, chunks them, embeds them, and stores them in the VectorStore.
// FLOW: HTTP request -> @RequestBody deserializes JSON into an
// IngestRequest -> @Valid triggers bean-validation (see IngestRequest's
// annotations) BEFORE this method body even runs -- an invalid request
// never reaches ingestionService.ingest() -- -> IngestionController
// delegates to IngestionService -> DocumentLoader -> Chunker ->
// EmbeddingModel -> VectorStore.
// WHY the controller has almost no logic of its own: this is the
// Controller-Service separation pattern -- the controller's only job is
// HTTP plumbing (deserialize request, call the service, wrap the result
// in a 200 OK). All actual business logic (loading files, chunking,
// indexing) lives in IngestionService, which makes that logic reusable
// and testable independent of any HTTP framework.
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
