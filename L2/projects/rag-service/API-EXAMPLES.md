# rag-service API Examples

Not executable in this sandbox (Maven Central blocked — see pom.xml and README.md), but these are the exact calls the wired controllers support.

## 1. Ingest the demo corpus

```
POST http://localhost:8081/api/v1/rag/ingest
Content-Type: application/json

{
  "corpusDirectory": "/absolute/path/to/rag-core/corpus"
}
```

Response:
```json
{
  "documentsLoaded": 7,
  "chunksIndexed": 37,
  "chunkSizeTokens": 180,
  "overlapTokens": 40
}
```
(These are the real numbers from `reports/retrieval-demo-run-log.txt`, produced by the equivalent logic run via `Main.java`.)

## 2. Search — hybrid (default)

```
GET http://localhost:8081/api/v1/rag/search?query=What+is+the+interest+rate+range+for+a+home+loan&method=HYBRID&topK=3
```

## 3. Search — the out-of-scope guardrail case

```
GET http://localhost:8081/api/v1/rag/search?query=Should+I+invest+my+savings+in+mutual+funds+right+now&method=SEMANTIC&topK=3
```

With only the absolute threshold (0.15), this query's real top score of 0.224 would NOT trigger the guardrail (see `reports/hallucination-risk-analysis.md`). This service adds the `min-score-margin` check (Recommendation 2 of that report) as a second guardrail layer — if the top result isn't meaningfully ahead of the runner-up, `guardrailTriggered` is still set to `true` even though the absolute threshold passed. Both fields are returned so a caller can see exactly which check fired.

## 4. Search — keyword only (baseline for comparison)

```
GET http://localhost:8081/api/v1/rag/search?query=What+happens+if+I+withdraw+my+fixed+deposit+before+maturity&method=KEYWORD&topK=3
```
