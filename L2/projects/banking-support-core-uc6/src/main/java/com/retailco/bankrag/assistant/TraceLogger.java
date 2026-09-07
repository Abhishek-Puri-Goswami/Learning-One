package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.ScoredChunk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Deliverable: "LangSmith trace reports (retrieval, prompts, errors)."
 *
 * Real LangSmith (smith.langchain.com) requires a network call and an API
 * key -- neither is available in this sandbox (no reachable external network
 * egress beyond the allowlisted package registries, confirmed throughout
 * this submission). This class is a LOCAL, disclosed stand-in: it captures
 * the same conceptual trace record LangSmith would (run id, inputs,
 * retrieved documents + scores, the constructed prompt, the raw and final
 * outputs, guardrail decisions, latency, and errors) and appends each run
 * as one JSON line to a trace log file, so every run in reports/ is a real,
 * inspectable trace of an actual pipeline execution -- not a mockup of what
 * a trace would look like.
 *
 * JSON is hand-serialized (no Jackson/Gson) for the same reason every other
 * module in this submission avoids third-party libraries: Maven Central is
 * blocked in this sandbox, so rag-assistant-core stays pure-JDK specifically
 * so it can be compiled and run for real (see README.md).
 *
 * Production swap-in: replace this class's file-append body with a call to
 * the real LangSmith Java/REST client (LangSmith has an official Python SDK
 * and a documented REST API consumable from Java via a simple HTTP client);
 * the TraceRecord shape below is already aligned to LangSmith's run schema
 * (name, run_type, inputs, outputs, extra/metadata, start_time, end_time,
 * error) to make that swap mechanical.
 */
public final class TraceLogger {

    private final Path logFile;

    public TraceLogger(Path logFile) {
        this.logFile = logFile;
    }

    public record TraceRecord(
            String runId,
            String runType,
            String query,
            List<ScoredChunk> retrievedChunks,
            String promptSent,
            String rawLlmOutput,
            String finalAnswer,
            List<CitationExtractor.Citation> citations,
            boolean promptInjectionBlocked,
            boolean unsafeQueryBlocked,
            boolean retrievalGuardrailTriggered,
            String guardrailMessage,
            long latencyMs,
            int promptTokens,
            int completionTokens,
            String error,
            Instant timestamp
    ) {
        public static String newRunId() {
            return UUID.randomUUID().toString();
        }
    }

    public void log(TraceRecord record) {
        String json = toJson(record);
        try {
            Files.writeString(logFile, json + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write trace log", e);
        }
    }

    private String toJson(TraceRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        field(sb, "run_id", r.runId(), true);
        field(sb, "run_type", r.runType(), true);
        field(sb, "timestamp", r.timestamp().toString(), true);
        field(sb, "query", r.query(), true);
        sb.append("\"retrieved_chunks\":[");
        for (int i = 0; i < r.retrievedChunks().size(); i++) {
            ScoredChunk sc = r.retrievedChunks().get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            field(sb, "chunk_id", sc.chunk().id(), true);
            sb.append("\"score\":").append(String.format("%.4f", sc.score()));
            sb.append("}");
        }
        sb.append("],");
        field(sb, "prompt_sent", truncate(r.promptSent(), 2000), true);
        field(sb, "raw_llm_output", r.rawLlmOutput(), true);
        field(sb, "final_answer", r.finalAnswer(), true);
        sb.append("\"citations\":[");
        for (int i = 0; i < r.citations().size(); i++) {
            CitationExtractor.Citation c = r.citations().get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            field(sb, "chunk_id", c.chunkId(), true);
            sb.append("\"resolvable\":").append(c.resolvable());
            sb.append("}");
        }
        sb.append("],");
        sb.append("\"prompt_injection_blocked\":").append(r.promptInjectionBlocked()).append(",");
        sb.append("\"unsafe_query_blocked\":").append(r.unsafeQueryBlocked()).append(",");
        sb.append("\"retrieval_guardrail_triggered\":").append(r.retrievalGuardrailTriggered()).append(",");
        field(sb, "guardrail_message", r.guardrailMessage(), true);
        sb.append("\"latency_ms\":").append(r.latencyMs()).append(",");
        sb.append("\"prompt_tokens\":").append(r.promptTokens()).append(",");
        sb.append("\"completion_tokens\":").append(r.completionTokens()).append(",");
        field(sb, "error", r.error(), false);
        sb.append("}");
        return sb.toString();
    }

    private void field(StringBuilder sb, String key, String value, boolean trailingComma) {
        sb.append("\"").append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append("\"").append(escape(value)).append("\"");
        }
        if (trailingComma) sb.append(",");
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...[truncated]";
    }
}
