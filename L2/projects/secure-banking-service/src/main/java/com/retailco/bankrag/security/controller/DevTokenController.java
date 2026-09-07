package com.retailco.bankrag.security.controller;

import com.retailco.bankrag.security.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * DEV/DEMO ONLY -- issues a token for a given customerId with no
 * credential check at all. A real system authenticates the customer
 * (password, OTP, biometric, SSO) through a proper login endpoint BEFORE
 * issuing a token; this endpoint exists solely so this API's JWT-protected
 * endpoints can be exercised end-to-end in a demo/local environment
 * without building a full login flow, which is out of L2 UC3's scope
 * (that belongs to a customer identity/auth service this use case assumes
 * already exists upstream, per L2 HLD UseCase3's framing: "Customers may
 * request..." implies an already-authenticated customer session).
 *
 * @Profile("dev") ensures this is never active in a "prod" profile
 * deployment -- see security/security-validation-checklist.md's item on
 * this exact risk (a demo/dev endpoint accidentally shipping to production).
 */
@RestController
@Profile("dev")
public class DevTokenController {

    private final JwtService jwtService;

    public DevTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/dev/token")
    public ResponseEntity<Map<String, String>> issueDevToken(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "CUSTOMER") String role) {
        String token = jwtService.issueToken(customerId, List.of(role));
        return ResponseEntity.ok(Map.of("token", token, "warning",
                "DEV-ONLY endpoint -- never expose this in production. See security/security-validation-checklist.md."));
    }
}
