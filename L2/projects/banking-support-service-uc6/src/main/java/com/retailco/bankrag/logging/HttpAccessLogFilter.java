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

// CONCEPT: Cross-cutting HTTP request/response logging via a Spring
// Security Filter (same base class and "runs once per request" mechanism
// as JwtAuthenticationFilter, but for observability rather than auth).
// PURPOSE: Logs one JSON line per HTTP request: method, path, status code,
// and duration -- basic access-log data useful for debugging and traffic
// analysis, generated automatically for every endpoint without each
// controller needing to log anything itself.
// HOW IT WORKS: wraps filterChain.doFilter() in a try/finally, so the log
// line is written exactly once per request regardless of whether the
// request succeeded, failed, or threw an exception (finally always runs).
// A random correlationId ties this log line to the SAME request's audit
// events (StructuredAuditLogger) and any other logs -- useful for tracing
// one request's full story across multiple log files.
// IMPORTANT (same PII-safety discipline as StructuredAuditLogger): only
// request METADATA is logged (method, path, status, timing) -- never
// headers (which would include the Authorization: Bearer token) or the
// request/response body (which could include account numbers). This is a
// deliberate, structural choice, not an oversight.
//
// One JSON line per HTTP request, written the same hand-rolled way as every
// other JSON-emitting class in this submission (no Jackson/Logstash-encoder
// reachable here -- see JwtService's Javadoc for the same reasoning applied
// to JWT payloads). Deliberately logs only request metadata (method, path,
// status, duration, a generated correlation id) -- never headers or body,
// so a Bearer token or a request payload containing account numbers can
// never end up in this log by construction, the same "no code path to leak
// PII" guarantee StructuredAuditLogger documents for the audit trail.
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
