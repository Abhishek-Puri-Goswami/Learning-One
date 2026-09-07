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
 * Lets an admin view the tail end of the structured audit log (the one
 * {@code BankingToolService} and {@code StructuredAuditLogger} write to)
 * for compliance and security review — exactly the kind of endpoint a
 * real audit team would need.
 * <p>
 * {@code @PreAuthorize("hasRole('ADMIN')")} is what protects it: Spring
 * Security checks whether the currently logged-in caller's roles include
 * ADMIN, and if not, rejects the request before {@code tail()} ever
 * runs — the method itself never has to check this manually. This
 * endpoint's rule is simple and fixed ("admin only, no exceptions"), so
 * a plain annotation is the clearest way to express it, unlike
 * {@code SupportController}, which needs the more flexible
 * {@code AccessPolicy} because its access rules depend on which customer
 * is being asked about. A plain {@code CUSTOMER} token — even a
 * perfectly valid one — can never reach this endpoint, and that's
 * enforced by the framework itself rather than relying on anyone to
 * remember a manual check.
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
