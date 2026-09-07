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

/**
 * Deliverable: RBAC demonstrated at the Spring Security annotation layer
 * (see SecurityConfig's Javadoc for why this endpoint, unlike
 * /api/v1/support/ask, uses {@code @PreAuthorize} directly instead of
 * delegating to AccessPolicy). ADMIN-only: tails the structured audit log
 * that BankingToolService writes on every access decision -- exactly the
 * kind of endpoint a real compliance/security team would need, and exactly
 * the kind of endpoint that must never be reachable by a plain CUSTOMER
 * token, even a valid one.
 */
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
