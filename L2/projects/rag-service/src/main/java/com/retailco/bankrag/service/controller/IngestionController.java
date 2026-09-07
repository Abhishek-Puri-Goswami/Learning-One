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
 * The thin, HTTP-facing entry point for {@code POST /api/v1/rag/ingest},
 * which loads a directory of documents, chunks them, embeds them, and
 * stores them for later searching.
 * <p>
 * Notice {@code @Valid} on the request parameter below — that triggers
 * Spring's validation of {@code IngestRequest}'s fields BEFORE this
 * method's body even runs, so an invalid request never even reaches
 * {@code ingestionService.ingest()}.
 * <p>
 * This controller has almost no logic of its own on purpose — that's the
 * "Controller-Service" separation pattern. The controller's only job is
 * HTTP plumbing (read the request, call the service, wrap the result in a
 * response); all the actual work (loading files, chunking, indexing)
 * lives in {@code IngestionService}, which makes that logic reusable and
 * easy to test without needing any HTTP framework at all.
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
