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
 * Deliverable: "Secure API endpoint" for the fully integrated system --
 * the single front door L2 HLD UseCase5 describes: one endpoint that
 * transparently serves grounded policy answers (L2/UC2, cached and
 * observed per UC4) or JWT-secured live banking data (L2/UC3), chosen by
 * L2/UC3's IntentClassifier, with no separate endpoint per use case.
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

        AskResponse body = switch (response) {
            case IntegratedBankingAssistant.PolicyAnswer p -> new AskResponse("POLICY_ANSWER", p);
            case IntegratedBankingAssistant.LiveDataAnswer d -> new AskResponse("LIVE_DATA", d);
            case IntegratedBankingAssistant.AccessDenied a -> new AskResponse("ACCESS_DENIED", a);
            case IntegratedBankingAssistant.Ambiguous amb -> new AskResponse("AMBIGUOUS", amb);
        };
        return ResponseEntity.ok(body);
    }
}
