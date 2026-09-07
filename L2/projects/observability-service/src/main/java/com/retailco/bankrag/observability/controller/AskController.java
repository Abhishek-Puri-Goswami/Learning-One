package com.retailco.bankrag.observability.controller;

import com.retailco.bankrag.assistant.RagAssistant;
import com.retailco.bankrag.observability.ObservableRagAssistant;
import com.retailco.bankrag.observability.dto.AskRequest;
import com.retailco.bankrag.observability.dto.AskResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * L2 UC4's version of L2/UC2's AskController: same underlying pipeline,
 * now wrapped with caching, metrics, and cost tracking. A repeated query
 * within the cache TTL returns servedFromCache=true and does not touch
 * retrieval or generation at all.
 */
@RestController
@RequestMapping("/api/v1/observability")
public class AskController {

    private final ObservableRagAssistant observableRagAssistant;

    public AskController(ObservableRagAssistant observableRagAssistant) {
        this.observableRagAssistant = observableRagAssistant;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        ObservableRagAssistant.ObservedResponse observed = observableRagAssistant.ask(request.query());
        RagAssistant.AssistantResponse r = observed.response();
        AskResponse body = new AskResponse(r.query(), r.answer(), r.citations(), r.blocked(),
                r.blockReason(), r.fallback(), observed.servedFromCache(), r.evaluation());
        return ResponseEntity.ok(body);
    }
}
