package com.retailco.bankrag.assistant.controller;

import com.retailco.bankrag.assistant.dto.IngestRequest;
import com.retailco.bankrag.assistant.dto.IngestResponse;
import com.retailco.bankrag.assistant.service.AssistantIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CONCEPT/PURPOSE: same thin Controller-layer pattern as rag-service's
// IngestionController (see that class's comments) -- POST
// /api/v1/assistant/ingest loads and indexes a corpus so AskController has
// something to retrieve from. Delegates all real work to
// AssistantIngestionService.
@RestController
@RequestMapping("/api/v1/assistant")
public class IngestionController {

    private final AssistantIngestionService ingestionService;

    public IngestionController(AssistantIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest(@Valid @RequestBody IngestRequest request) {
        return ResponseEntity.ok(ingestionService.ingest(request));
    }
}
