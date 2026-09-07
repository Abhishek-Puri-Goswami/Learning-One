package com.retailco.bankrag.integration.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

// CONCEPT: Method-level security with `@PreAuthorize` -- Spring Security's
// annotation-based authorization check, evaluated BEFORE the method body
// runs.
// PURPOSE: Exposes the structured audit log (written by
// BankingToolService/StructuredAuditLogger) for compliance/security
// review -- exactly the kind of endpoint a real audit team would need.
// HOW @PreAuthorize("hasRole('ADMIN')") WORKS: Spring Security intercepts
// the call and checks whether the currently authenticated user's
// authorities (set by JwtAuthenticationFilter from the JWT's roles,
// prefixed "ROLE_") include ROLE_ADMIN. If not, the request is rejected
// with a 403 before `tail()` ever executes -- the method body never has
// to check this itself.
// WHY THIS DIFFERS FROM SupportController (which relies on AccessPolicy
// for its authorization logic instead): this endpoint's rule is simple
// and fixed ("ADMIN only, no exceptions"), so a declarative annotation is
// the clearest way to express it. AccessPolicy exists for the MORE
// complex, business-specific "who can see whose banking data" decision
// that a single annotation couldn't express as clearly.
// IMPORTANT: this endpoint must never be reachable by a plain CUSTOMER
// token, even a technically valid one -- @PreAuthorize enforces that
// structurally, at the framework level, rather than relying on every
// caller to remember a manual check.
@RestController
public class AuditController {

    private final Path auditLogPath;

    public AuditController(Path auditLogPath) {
        this.auditLogPath = auditLogPath;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/audit-log/tail")
    public ResponseEntity<Map<String, Object>> tail(@RequestParam(defaultValue = "20") int lines) throws IOException {
        if (!Files.exists(auditLogPath)) {
            return ResponseEntity.ok(Map.of("lines", List.of(), "note", "No audit events recorded yet."));
        }
        List<String> all = Files.readAllLines(auditLogPath);
        List<String> tail = all.size() <= lines ? all : all.subList(all.size() - lines, all.size());
        return ResponseEntity.ok(Map.of("totalEvents", all.size(), "lines", tail));
    }
}
