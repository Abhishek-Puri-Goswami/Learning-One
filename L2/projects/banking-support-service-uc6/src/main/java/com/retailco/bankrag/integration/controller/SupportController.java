package com.retailco.bankrag.integration.controller;

import com.retailco.bankrag.integration.IntegratedBankingAssistant;
import com.retailco.bankrag.integration.dto.AskRequest;
import com.retailco.bankrag.integration.dto.AskResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single front door for the whole integrated system: {@code POST
 * /api/v1/support/ask} handles both policy questions and live banking
 * data questions through one endpoint. It doesn't decide which is which
 * itself — it just passes the request to {@code IntegratedBankingAssistant},
 * which does the routing, and then wraps whatever comes back into one
 * {@code AskResponse} shape with a {@code type} field, so the same JSON
 * response shape can represent any of the four possible kinds of
 * outcome.
 * <p>
 * This uses a chain of {@code instanceof} checks rather than Java's
 * newer pattern-matching {@code switch} syntax, so the project compiles
 * with a plain {@code mvn compile} without needing to enable a preview
 * language feature.
 */
@RestController
@RequestMapping("/api/v1/support")
public class SupportController {

    private final IntegratedBankingAssistant assistant;

    public SupportController(IntegratedBankingAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request, HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        IntegratedBankingAssistant.UnifiedResponse response = assistant.handle(
                request.query(), authHeader, request.requestedCustomerId(), request.accountNumber());

        // NOTE: uses instanceof pattern matching (stable since Java 16), not a
        // pattern-matching switch (still a preview feature as of Java 17) --
        // compiles with plain `mvn compile`, no --enable-preview flag.
        AskResponse body;
        if (response instanceof IntegratedBankingAssistant.PolicyAnswer p) {
            body = new AskResponse("POLICY_ANSWER", p);
        } else if (response instanceof IntegratedBankingAssistant.LiveDataAnswer d) {
            body = new AskResponse("LIVE_DATA", d);
        } else if (response instanceof IntegratedBankingAssistant.AccessDenied a) {
            body = new AskResponse("ACCESS_DENIED", a);
        } else if (response instanceof IntegratedBankingAssistant.Ambiguous amb) {
            body = new AskResponse("AMBIGUOUS", amb);
        } else {
            throw new IllegalStateException("Unknown response type: " + response.getClass());
        }
        return ResponseEntity.ok(body);
    }
}
