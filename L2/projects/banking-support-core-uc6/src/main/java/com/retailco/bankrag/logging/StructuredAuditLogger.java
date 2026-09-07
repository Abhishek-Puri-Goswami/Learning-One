package com.retailco.bankrag.logging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deliverable: "PII masking & structured logging" / "secure logging flow."
 * L2 HLD UseCase6 Implementation Approach step 3: "Implement masking,
 * structured logs, and secure logging flow."
 *
 * This is deliberately a separate concern from L2/UC2's {@code TraceLogger}
 * (which logs RAG generation traces for evaluation) and from UC3's
 * {@code PiiMasking} (which masks values inside API *responses*). This
 * class is the audit trail for *access decisions* -- who asked for whose
 * data, under which role, and whether it was allowed -- written as one JSON
 * object per line (JSONL), the same log-shipping-friendly shape UC2/UC4
 * used for trace/observability records.
 *
 * Structural guarantee, not just a convention: every field this class ever
 * writes is one of {@code timestamp, correlationId, event, actorSubject,
 * actorRoles, requestedCustomerId, tool, decision, reason}. None of those
 * are raw PII (account numbers, balances, mobile numbers, government IDs) --
 * this class has no method that accepts a free-form message string, so
 * there is no code path by which a caller could accidentally log raw
 * customer data through it. That is a stronger guarantee than "we remember
 * to mask before logging" would be.
 */
public final class StructuredAuditLogger {

    private final Path logFile;

    public StructuredAuditLogger(Path logFile) {
        this.logFile = logFile;
    }

    public record AuditEvent(
            String correlationId,
            String event,
            String actorSubject,
            List<String> actorRoles,
            String requestedCustomerId,
            String tool,
            String decision,
            String reason
    ) {
    }

    public synchronized void log(AuditEvent event) {
        String line = toJson(event);
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            Files.writeString(logFile, line + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write structured audit log", e);
        }
    }

    private String toJson(AuditEvent e) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", Instant.now().toString());
        fields.put("correlationId", e.correlationId());
        fields.put("event", e.event());
        fields.put("actorSubject", e.actorSubject());
        fields.put("actorRoles", e.actorRoles());
        fields.put("requestedCustomerId", e.requestedCustomerId());
        fields.put("tool", e.tool());
        fields.put("decision", e.decision());
        fields.put("reason", e.reason());

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(renderValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderValue(Object v) {
        if (v == null) return "null";
        if (v instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escape(String.valueOf(list.get(i)))).append("\"");
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
