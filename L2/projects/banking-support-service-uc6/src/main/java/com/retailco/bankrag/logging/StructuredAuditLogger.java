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

// CONCEPT: Security audit logging -- structured, append-only records of
// every access DECISION (not the data itself), designed so PII cannot leak
// into logs even by accident.
// PURPOSE: Every call into BankingToolService ends with one AuditEvent
// written here: who asked (actorSubject/actorRoles), for whose data
// (requestedCustomerId), which tool, and the decision
// (ALLOWED_SELF/ALLOWED_ELEVATED/DENIED_AUTHENTICATION/
// DENIED_AUTHORIZATION) with a reason.
//
// IMPORTANT (a structural, not just conventional, PII guarantee): this
// class has NO method that accepts a free-form message string -- `log()`
// only accepts the fixed AuditEvent record, whose fields are all
// identifiers/decisions/roles, never raw account numbers, balances, or
// other PII. That means there is literally no code path through this
// class by which a caller could accidentally log sensitive customer data
// -- a stronger guarantee than "developers remember to mask before
// logging," because it doesn't depend on anyone remembering anything.
//
// WHY separate from TraceLogger (assistant package) and PiiMasking
// (security package): TraceLogger records RAG generation traces for
// evaluation; PiiMasking hides sensitive fields inside API *responses*.
// This class's only concern is the audit trail of access DECISIONS -- a
// distinct responsibility that deserves its own class rather than being
// bolted onto either of those.
//
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
