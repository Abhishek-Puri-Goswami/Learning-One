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

// CONCEPT/PURPOSE: same Controller-layer pattern as rag-assistant-service's
// AskController, but delegating to ObservableRagAssistant (the caching/
// metrics/cost decorator) instead of RagAssistant directly -- the
// controller itself doesn't need to know caching exists; it just reads
// `observed.servedFromCache()` to include that fact in the response.
// A repeated query within the cache TTL returns servedFromCache=true and
// skips retrieval/generation entirely (see QueryCache/ObservableRagAssistant).
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
