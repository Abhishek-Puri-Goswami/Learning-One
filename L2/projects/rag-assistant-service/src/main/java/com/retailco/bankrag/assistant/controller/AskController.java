package com.retailco.bankrag.assistant.controller;

import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.assistant.dto.AskRequest;
import com.retailco.bankrag.assistant.dto.AskResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CONCEPT: Controller layer -- the REST entry point for the entire RAG
// assistant pipeline.
// PURPOSE: POST /api/v1/assistant/ask takes a user's question and runs
// RagAssistant's full pipeline (guardrails -> retrieval -> prompt ->
// generation -> citation -> trace -> evaluation) for it, then maps the
// internal `AssistantResponse` onto the public `AskResponse` DTO.
// WHY map AssistantResponse -> AskResponse here rather than returning
// AssistantResponse directly: it keeps the internal RagAssistant class
// free to evolve its own return shape without being constrained by "this
// is also our public API contract" -- the controller is the one place
// that decides what's actually exposed over HTTP.
@RestController
@RequestMapping("/api/v1/assistant")
public class AskController {

    private final RagAssistant ragAssistant;

    public AskController(RagAssistant ragAssistant) {
        this.ragAssistant = ragAssistant;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        RagAssistant.AssistantResponse response = ragAssistant.ask(request.query());
        AskResponse body = new AskResponse(response.query(), response.answer(), response.citations(),
                response.blocked(), response.blockReason(), response.fallback(), response.evaluation());
        return ResponseEntity.ok(body);
    }
}
