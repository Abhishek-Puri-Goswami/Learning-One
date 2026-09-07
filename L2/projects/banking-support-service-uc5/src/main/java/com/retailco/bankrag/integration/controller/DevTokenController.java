package com.retailco.bankrag.integration.controller;

import com.retailco.bankrag.security.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** DEV/DEMO ONLY -- same disclosed limitation as L2/UC3's DevTokenController. Never active outside the "dev" profile. */
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
