package com.retailco.bankrag.integration.controller;

import com.retailco.bankrag.security.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * A DEV/DEMO-ONLY endpoint that hands out a signed login token for any
 * customer id and role you ask for — purely so this demo can generate
 * test tokens without needing a real login screen.
 * <p>
 * {@code @Profile("dev")} is what keeps this safe: Spring only creates
 * this controller when the "dev" profile is turned on. In a real
 * production deployment, this class is never even registered, so there's
 * no way to reach it — this is stronger than just a comment saying
 * "don't call this in production," because the endpoint genuinely
 * doesn't exist unless the dev profile is active. A real system would
 * need an actual login flow (username/password, OAuth, etc.) to issue
 * tokens instead of an open endpoint that hands one out on request.
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
            @RequestParam String customerId, @RequestParam(defaultValue = "CUSTOMER") String role) {
        String token = jwtService.issueToken(customerId, List.of(role));
        return ResponseEntity.ok(Map.of("token", token, "warning", "DEV-ONLY endpoint -- never expose in production."));
    }
}
