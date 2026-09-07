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
 * The core L2 UC2 deliverable as a REST endpoint: POST /api/v1/assistant/ask
 * runs the full guardrail -> retrieval -> prompt -> generation -> citation
 * -> trace -> evaluation pipeline (RagAssistant.ask) for a single query.
 *
 * See rag-assistant-core/API-EXAMPLES.md for example requests, including
 * the guardrail-triggering and fallback-triggering cases exercised for
 * real in reports/assistant-demo-run-log.txt.
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
