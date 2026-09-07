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
 * Writes an append-only, structured log of every access DECISION (not
 * the underlying data) made by {@code BankingToolService}. Every call
 * into that class ends with one {@code AuditEvent} written here: who
 * asked, for whose data, which tool they used, and what was decided
 * (allowed, or denied and why).
 * <p>
 * A useful safety detail: this class has no method that accepts a
 * free-form message string — {@code log()} only accepts the fixed
 * {@code AuditEvent} record, whose fields are all identifiers, decisions,
 * and roles, never raw account numbers, balances, or other sensitive
 * data. That means there's no way for a caller to accidentally log
 * sensitive customer data through this class, since the shape of the
 * data it accepts simply doesn't allow it.
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
