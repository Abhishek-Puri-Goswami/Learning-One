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

// CONCEPT: Controller layer -- the single unified REST entry point for
// the whole integrated system (one endpoint, many possible outcomes).
// PURPOSE: POST /api/v1/support/ask is the ONE front door for both policy
// questions and live banking data -- IntegratedBankingAssistant decides
// internally which subsystem actually handles each request.
// FLOW: extract the raw Authorization header (only actually used for
// live-data intents -- see IntegratedBankingAssistant's Javadoc) ->
// delegate to assistant.handle(...) -> convert whichever UnifiedResponse
// subtype came back into one AskResponse DTO with a `type` discriminator
// field, so a single JSON response shape can represent 4 different kinds
// of outcome.
// WHY instanceof pattern matching (not a pattern-matching switch): this
// was written to compile with plain `mvn compile` under this project's
// Java version without needing an `--enable-preview` flag -- see the
// inline note in the code for the exact reasoning.
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
