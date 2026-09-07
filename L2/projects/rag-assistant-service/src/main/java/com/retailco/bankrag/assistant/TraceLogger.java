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

// CONCEPT: Observability / tracing -- a local, file-based stand-in for a
// distributed tracing system (LangSmith-style: one structured record per
// pipeline run).
// PURPOSE: Every call to RagAssistant.ask() ends by writing one JSON line
// here, capturing EVERYTHING about that run: the query, what was
// retrieved (with scores), the exact prompt sent to the LLM, the raw and
// final answer, citations, which guardrail (if any) fired, latency, and
// token counts. This turns every run into something you can debug and
// audit after the fact, instead of only seeing the final answer.
//
// HOW IT WORKS: `log()` serializes a TraceRecord to a single-line JSON
// string (toJson(), hand-written -- see the field()/escape() helpers) and
// appends it to a file with StandardOpenOption.APPEND, so the file grows
// into a JSON-Lines (.jsonl) log where each line is one independent,
// parseable run record.
//
// WHY append to a plain file instead of a real tracing service: this
// keeps the module dependency-free and instantly runnable, while still
// producing REAL, inspectable evidence of every run (not a mockup). The
// TraceRecord's field names (run_id, run_type, inputs/outputs, latency,
// error) intentionally mirror LangSmith's actual run schema, so swapping
// this class's file-write for a real LangSmith API call would be a
// mechanical, low-risk change -- nothing calling `log()` would need to
// change.
//
// WHAT IF REMOVED: the assistant would still function correctly, but
// every run's history (what was retrieved, why a guardrail fired, how
// long it took) would be lost the moment the response was returned --
// no way to debug a bad answer after the fact.
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
