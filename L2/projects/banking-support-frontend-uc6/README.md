# banking-support-frontend

Deliverable: "Frontend Interface" (L2 HLD UseCase6, section 8.2) —
"Policy query UI" and "Banking dashboard (balance/cards/loans)." React 19 +
Vite, plain fetch (no client HTTP library needed), talking to exactly one
backend endpoint: `POST /api/v1/support/ask` (see `src/api.js`).

## What's real vs. mock, honestly

- **The frontend itself is real, buildable, and was actually run** — this
  is the one part of this submission where the sandbox's Maven Central
  block does not apply (npm's registry IS reachable here). `npm run build`
  and `npm run lint` (Oxlint) both pass clean; see `../reports/frontend-build-log.txt`.
- **It was driven end-to-end with Playwright** against `mock-server/server.js`
  (a zero-dependency JSON-contract-matching stand-in for the real Spring
  Boot backend, which this sandbox cannot build) — screenshots in
  `../reports/frontend-screenshots/` show the policy Q&A flow and the
  banking dashboard actually rendering real masked account data returned
  over real HTTP, not a static mock-up.
- **It has never been run against the real `banking-support-service`** —
  that module isn't compile-verified here either (same Maven Central
  block). The contract match is source-level: every field name in
  `src/api.js`'s expected response shape was copied directly from
  `SupportController.java`/`AskResponse.java`/`RagAssistant.AssistantResponse`/
  `BankingToolService`'s masked record types, not guessed.

## Run it

```bash
cp .env.example .env
npm install

# Option A: against the mock backend (works in any environment, including this sandbox)
node mock-server/server.js &
npm run dev            # http://localhost:5173

# Option B: against the real backend (needs Maven Central reachable)
cd ../banking-support-service && mvn spring-boot:run &
cd ../banking-support-frontend && npm run dev
```

## Structure

| Path | Purpose |
|---|---|
| `src/api.js` | The one HTTP contract this app speaks — `askSupport`, `fetchDevToken`, `fetchAuditTail` |
| `src/components/PolicyQueryView.jsx` | Deliverable 8.2's "Policy query UI" |
| `src/components/BankingDashboardView.jsx` | Deliverable 8.2's "Banking dashboard" — balance, transactions, loan, RBAC-aware (SUPPORT_AGENT/ADMIN can view another customer's masked data) |
| `mock-server/` | Frontend-only mock backend — see its own README |

## Build & lint

```bash
npm run build   # vite build -- verified: builds clean, ~200KB gzip ~62KB
npm run lint    # oxlint -- verified: 0 issues
```
