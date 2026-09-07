package com.retailco.emailagent.logging;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Per the HLD's "PII redaction in logs" guardrail: this agent's logs must
 * never contain a raw email address or the raw body text of a message --
 * only redacted summaries. Structural, like L2/UC6's
 * {@code StructuredAuditLogger}: this class's {@link #redact} method is
 * the only path anything reaches a log line through, and it always runs
 * the redaction regex before returning -- there is no bypass method that
 * writes raw text.
 */
public class PiiRedactingLogger {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private final List<String> lines = new java.util.ArrayList<>();

    public void log(String message) {
        lines.add(redact(message));
    }

    public String redact(String message) {
        if (message == null) {
            return "";
        }
        Matcher m = EMAIL_PATTERN.matcher(message);
        return m.replaceAll("[REDACTED_EMAIL]");
    }

    public List<String> getLines() {
        return List.copyOf(lines);
    }
}
