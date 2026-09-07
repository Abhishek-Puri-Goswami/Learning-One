package com.retailco.bankrag.integration.controller;

import com.retailco.bankrag.security.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// CONCEPT: Spring Profiles (`@Profile`) -- conditionally registering a
// bean/controller only when a specific named profile is active.
// PURPOSE: Issues a signed JWT for a given customerId/role, purely so the
// demo/dev environment can generate test tokens without a real login flow.
// WHY @Profile("dev") matters: this endpoint would be a serious security
// hole in production (anyone could mint a token for any customer id with
// any role) -- @Profile("dev") means Spring only registers this
// controller when the "dev" profile is explicitly active
// (spring.profiles.active=dev), so it's structurally absent from a
// production deployment rather than merely "not supposed to be called."
// IMPORTANT: this is disclosed as DEV/DEMO ONLY -- a real system needs an
// actual authentication flow (login, OAuth, etc.) issuing tokens, not an
// open endpoint that hands one out on request.
@RestController
@Profile("dev")
public class DevTokenController {

    private final JwtService jwtService;

    public DevTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/dev/token")
    public ResponseEntity<Map<String, String>> issueDevToken(
            @RequestParam String customerId, @RequestParam(defaultValue = "CUSTOMER") String role) {
        String token = jwtService.issueToken(customerId, List.of(role));
        return ResponseEntity.ok(Map.of("token", token, "warning", "DEV-ONLY endpoint -- never expose in production."));
    }
}
