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

/**
 * The REST entry point for the whole RAG assistant pipeline. Sending
 * {@code POST /api/v1/assistant/ask} runs {@code RagAssistant}'s full
 * pipeline (guardrails → retrieval → prompt → answer → citations →
 * tracing → evaluation) on the question, then maps the internal
 * {@code AssistantResponse} onto the public {@code AskResponse} DTO.
 * <p>
 * Why bother converting between two very similar-looking types instead
 * of just returning {@code AssistantResponse} directly: it means
 * {@code RagAssistant} stays free to change its own internal return shape
 * later without breaking our public API — this controller is the one
 * place that decides exactly what gets exposed over HTTP.
 */
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
