# Mock backend (frontend development only)

`server.js` is a zero-dependency Node `http` server that mirrors
`banking-support-service`'s exact JSON contract (`POST /api/v1/support/ask`,
`GET /dev/token`, `GET /api/v1/admin/audit-log/tail`) so the React frontend
can be run and demoed without a live Spring Boot instance — which this
submission's sandbox cannot build (Maven Central is blocked here; see the
top-level README).

**This is not the real backend.** It does not use `JwtService`'s real
HS256 implementation, `AccessPolicy`, `RagAssistant`, or any of the
actually-compiled-and-run `banking-support-core` logic. Tokens it issues
are the literal string `MOCK.<customerId>.<role>` — not a signed JWT.
Policy answers are a fixed placeholder string, not real retrieval.

What it DOES prove, and did prove during this submission's own build:

- The frontend's `fetch` calls, request bodies, and response-shape
  handling are correct against the real backend's documented contract
  (`SupportController.java`, `AskRequest`/`AskResponse`, `RagAssistant.AssistantResponse`,
  `BankingToolService`'s masked record types) — every field name here was
  copied from that Java source, not guessed.
- The full 3-tier flow (React UI → HTTP → JSON response → rendered table/answer)
  actually runs, was actually screenshotted (see `../../reports/frontend-screenshots/`
  if included, or re-run with `node mock-server/server.js` + `npm run dev`).

## Run it

```bash
node mock-server/server.js     # listens on :8085, same port the real service uses
# in another terminal:
cp .env.example .env
npm install
npm run dev                    # http://localhost:5173
```

## Swap in the real backend

Once `banking-support-service` builds in an environment with Maven Central
reachable (`cd ../banking-support-service && mvn spring-boot:run`), just
stop the mock server — the frontend's `.env` already points at
`http://localhost:8085` either way, since the real service uses the same
port and the same contract this mock was built to match.
