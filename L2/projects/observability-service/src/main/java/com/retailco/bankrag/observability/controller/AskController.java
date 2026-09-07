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
 * Same idea as the plain {@code AskController} in other modules, but this
 * one talks to {@code ObservableRagAssistant} (the caching/metrics/cost
 * decorator) instead of {@code RagAssistant} directly. The controller
 * itself doesn't need to know caching even exists — it just reads
 * {@code observed.servedFromCache()} to report whether this particular
 * answer came from the cache or was freshly generated.
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
