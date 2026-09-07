package com.retailco.emailagent.observability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Hand-rolled JSONL trace logger -- same disclosed local-file stand-in
 * pattern L2/UC2 introduced for its LangSmith-equivalent tracing (this
 * sandbox has no reachable LangSmith endpoint). One line per agent step
 * (intent classification, calendar lookup, draft composed, dedupe
 * decision), so a run can be replayed step-by-step from the log file.
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
