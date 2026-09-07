package com.retailco.bankrag.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

/**
 * A filter that runs once per HTTP request and writes one JSON log line
 * for it: the method, path, status code, and how long it took. This
 * gives basic access-log data useful for debugging and traffic analysis,
 * generated automatically for every endpoint, without any controller
 * needing to log anything itself.
 * <p>
 * It wraps {@code filterChain.doFilter()} in a try/finally, so the log
 * line gets written exactly once per request no matter whether the
 * request succeeded, failed, or threw an exception. A randomly generated
 * correlation id ties this log line to that same request's audit events
 * in {@code StructuredAuditLogger} — handy for tracing one request's full
 * story across different log files.
 * <p>
 * Just like {@code StructuredAuditLogger}, only request METADATA is
 * logged here (method, path, status, timing) — never headers (which
 * would include the Authorization token) or the request/response body
 * (which could include account numbers). That's a deliberate choice, not
 * an oversight.
 */
@Component
public class HttpAccessLogFilter extends OncePerRequestFilter {

    private final Path logFile = Path.of("reports", "http-access-log.jsonl");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString();
        request.setAttribute("correlationId", correlationId);
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            writeLine(correlationId, request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
        }
    }

    private void writeLine(String correlationId, String method, String path, int status, long durationMs) {
        String json = "{"
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"correlationId\":\"" + correlationId + "\","
                + "\"method\":\"" + method + "\","
                + "\"path\":\"" + escape(path) + "\","
                + "\"status\":" + status + ","
                + "\"durationMs\":" + durationMs
                + "}";
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            Files.writeString(logFile, json + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write structured HTTP access log", e);
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
