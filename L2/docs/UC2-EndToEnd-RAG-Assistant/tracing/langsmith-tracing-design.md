# LangSmith Trace Reports — Design Notes

Deliverable: "LangSmith trace reports (retrieval, prompts, errors)," per L2 HLD UseCase2.

## Why this is a local stand-in, not the real LangSmith

Real LangSmith (`smith.langchain.com`) requires outbound network access and an API key. This sandbox has no reachable general internet egress beyond the allowlisted package registries (confirmed repeatedly throughout this submission — same limitation documented for Maven Central, GitHub, and every LLM/embedding API). `TraceLogger.java` is a disclosed, functionally-equivalent local substitute: every `RagAssistant.ask()` call appends one structured JSON record to `reports/langsmith-style-trace-log.jsonl`, real output from real runs — see `reports/assistant-demo-run-log.txt` for the human-readable version of the same 6 runs.

## Schema, and its alignment to LangSmith's real run schema

Each trace record captures, per L2 HLD UseCase2's "Use LangSmith tracing to debug retrieval, prompts, and output quality":

| Field | Purpose | LangSmith equivalent |
|---|---|---|
| `run_id` | Unique id per query | `run.id` |
| `run_type` | Always `"chain"` here (the full guardrail→retrieval→prompt→generation pipeline) | `run.run_type` |
| `query` | The raw user input | `run.inputs.query` |
| `retrieved_chunks` | Every chunk id + score returned by the retriever, even when the guardrail ultimately blocks the answer | `run.outputs` of the retriever's child run |
| `prompt_sent` | The exact fully-assembled prompt (or a note that generation was skipped) | `run.inputs` of the LLM's child run |
| `raw_llm_output` / `final_answer` | What the (stub) model returned | `run.outputs` |
| `citations` | Parsed, resolved/unresolved citation list | custom `run.extra` metadata |
| `prompt_injection_blocked` / `unsafe_query_blocked` / `retrieval_guardrail_triggered` / `guardrail_message` | Which guardrail (if any) fired, and why | custom `run.extra` metadata / `run.tags` |
| `latency_ms` | Real wall-clock time for the whole `ask()` call | `run.end_time - run.start_time` |
| `prompt_tokens` / `completion_tokens` | Token estimates (whitespace-tokenizer approximation, same disclosed limitation as UC1's `Chunker.tokenize`) | `run.prompt_tokens` / `run.completion_tokens` |
| `error` | Reserved for exception messages (unused in this run — no runtime errors occurred) | `run.error` |

Field names were chosen to mirror LangSmith's actual run schema (`inputs`/`outputs`/`extra`/timestamps) specifically so that swapping this class's file-append body for a real `POST` to LangSmith's REST API (or the LangSmith Python SDK, called from Java via a lightweight HTTP client, since there is no official LangSmith Java SDK) is a mechanical change, not a redesign.

## Why hand-rolled JSON, not a library

No JSON library (Jackson, Gson) is used because Maven Central is blocked in this sandbox and `rag-assistant-core` is deliberately pure-JDK so it can be compiled and run for real here (same architectural choice as UC1's `rag-core`). `TraceLogger.toJson()` is a small, hand-written, escaping-aware serializer — verified correct by `SelfTests.testTraceLoggerWritesValidJsonLine` and by the fact that every line of the real `langsmith-style-trace-log.jsonl` produced by the demo run parses cleanly with Python's standard `json` module (checked as part of producing this deliverable).

## What the real trace log shows (see evaluation/rag-assistant-evaluation-summary.md for the full analysis)

Of the 6 demo queries: 2 were blocked pre-retrieval by guardrail layers 1–2 (`retrieved_chunks: []` in their trace records — proof the pipeline short-circuited before spending retrieval or generation cost), 2 triggered the retrieval-guardrail fallback (with the exact `topScore`/`margin` numbers that caused it recorded in `guardrail_message`), and 2 produced grounded, cited answers. Every one of these six outcomes is independently reconstructable from the trace log alone, without re-running the demo — which is the actual point of tracing.
