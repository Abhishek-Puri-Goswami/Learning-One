package com.retailco.emailagent.observability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Writes one line of a log file for every step the agent takes (checking
 * intent, looking up the calendar, composing a draft, checking for
 * duplicates). Because every step gets its own line, you can look back at
 * this file afterward and replay exactly what the agent did, and why, for
 * any given run.
 */
public class TraceLogger {

    private final Path logFile;

    public TraceLogger(Path logFile) {
        this.logFile = logFile;
    }

    public void log(String threadId, String step, String detail) {
        String json = "{"
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"threadId\":\"" + escape(threadId) + "\","
                + "\"step\":\"" + escape(step) + "\","
                + "\"detail\":\"" + escape(detail) + "\""
                + "}";
        try {
            Files.writeString(logFile, json + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedTraceLogException(e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static class UncheckedTraceLogException extends RuntimeException {
        UncheckedTraceLogException(Throwable cause) {
            super(cause);
        }
    }
}
