package com.retailco.emailagent.logging;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A logger that automatically hides sensitive information (email
 * addresses) before storing anything — so no one calling this class has
 * to remember to scrub the data themselves every single time.
 * <p>
 * Notice {@code log()} is the ONLY way to add a line, and it always calls
 * {@code redact()} first. There is no other method that stores raw,
 * unredacted text — which means this protection can't accidentally be
 * skipped by whoever uses this class.
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
