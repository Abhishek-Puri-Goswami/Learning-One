// MOCK backend for frontend-only local development. This is NOT
// banking-support-service -- it does not use JwtService's real HS256
// implementation, AccessPolicy, or RagAssistant at all. It exists purely
// so the React frontend in ../src can be exercised end-to-end without a
// live Spring Boot instance (which this submission's sandbox cannot
// build/run -- see ../../banking-support-service/README.md and the
// top-level README's "what's real vs. documented-but-unverified" section).
//
// The response SHAPES here are copied exactly from the real backend's
// Java records (SupportController/AskResponse, IntegratedBankingAssistant's
// UnifiedResponse variants, RagAssistant.AssistantResponse,
// BankingToolService's MaskedAccount/MaskedTransaction/MaskedLoan) --
// see each file's source for the authoritative definition this mock is
// intentionally kept in sync with. Zero npm dependencies (plain Node
// `http`), matching this submission's "no third-party library unless it's
// unavoidable" pattern used throughout the pure-JDK core modules.
//
// Run: node mock-server/server.js   (defaults to port 8085, same as the real service)

import http from "http";
import { URL } from "url";

const PORT = process.env.MOCK_PORT || 8085;

const ACCOUNTS = {
  CUST1001: [
    { accountNumberMasked: "XXXXXXXX0456", accountType: "SAVINGS", balance: 184250.75, currency: "INR" },
    { accountNumberMasked: "XXXXXXXX0789", accountType: "CURRENT", balance: 52310.00, currency: "INR" },
  ],
  CUST1002: [
    { accountNumberMasked: "XXXXXXXX0999", accountType: "SAVINGS", balance: 12800.00, currency: "INR" },
  ],
};
const TRANSACTIONS = {
  CUST1001: [
    { transactionId: "TXN9001", date: "2026-08-25", description: "Salary Credit", amount: 95000.00, type: "CREDIT" },
    { transactionId: "TXN9002", date: "2026-08-24", description: "Electricity Bill Payment", amount: -3200.50, type: "DEBIT" },
    { transactionId: "TXN9003", date: "2026-08-20", description: "ATM Withdrawal", amount: -10000.00, type: "DEBIT" },
    { transactionId: "TXN9004", date: "2026-08-15", description: "Interest Credit", amount: 412.25, type: "CREDIT" },
  ],
};
const LOANS = {
  CUST1001: [
    { loanId: "LOAN5001", loanType: "HOME_LOAN", principal: 4500000.00, outstanding: 3120450.00, interestRate: 8.75 },
  ],
};

const BALANCE_RE = /account balance|my balance|how much (money |)do i have|current balance/i;
const TRANSACTION_RE = /transaction history|recent transactions|my (recent |)transactions|statement of account|last \d+ transactions/i;
const LOAN_RE = /loan outstanding|outstanding (loan |)amount|how much (do i owe|is left) on my loan|remaining loan balance/i;
const INJECTION_RE = /ignore (all |any |the |previous |prior |above )+instructions?/i;
const UNSAFE_ADVICE_RE = /should i invest|which stocks|is it (a )?good time to invest|financial advice on/i;

function classify(query) {
  const matches = [BALANCE_RE.test(query), TRANSACTION_RE.test(query), LOAN_RE.test(query)];
  const count = matches.filter(Boolean).length;
  if (count > 1) return "AMBIGUOUS";
  if (matches[0]) return "ACCOUNT_BALANCE";
  if (matches[1]) return "TRANSACTION_HISTORY";
  if (matches[2]) return "LOAN_OUTSTANDING";
  return "POLICY_QUESTION";
}

// Decodes the mock token minted by /dev/token below: "MOCK.<customerId>.<role>"
function decodeMockToken(authHeader) {
  if (!authHeader || !authHeader.startsWith("Bearer ")) return null;
  const token = authHeader.slice("Bearer ".length);
  const parts = token.split(".");
  if (parts.length !== 3 || parts[0] !== "MOCK") return null;
  return { subject: parts[1], role: parts[2] };
}

function evaluateAccess(subject, role, requestedCustomerId) {
  if (subject === requestedCustomerId) return { allowed: true };
  if (role === "ADMIN" || role === "SUPPORT_AGENT") return { allowed: true };
  return { allowed: false, reason: `subject '${subject}' holds no role that permits access to customer '${requestedCustomerId}'` };
}

function policyAnswer(query) {
  if (INJECTION_RE.test(query)) {
    return { type: "POLICY_ANSWER", payload: { ragResponse: { query, answer: null, citations: [], blocked: true, blockReason: "Query matched prompt-injection pattern: ignore (all |any |the |previous |prior |above )+instructions?", fallback: false, evaluation: null }, servedFromCache: false } };
  }
  if (UNSAFE_ADVICE_RE.test(query)) {
    return { type: "POLICY_ANSWER", payload: { ragResponse: { query, answer: null, citations: [], blocked: true, blockReason: "Query requests personalized financial/investment/legal advice, which is outside a policy-document assistant's scope regardless of what retrieval returns.", fallback: false, evaluation: null }, servedFromCache: false } };
  }
  return {
    type: "POLICY_ANSWER",
    payload: {
      ragResponse: {
        query,
        answer: "[MOCK ANSWER -- run the real banking-support-service for a grounded, cited response] " +
          "This is a placeholder response from mock-server/server.js, not the real RagAssistant/ExtractiveStubLlmClient pipeline.",
        citations: [{ chunkId: "mock#0", sourceDocument: "mock_policy.txt", chunkIndex: 0, similarityScore: 0.42, resolvable: true }],
        blocked: false,
        blockReason: null,
        fallback: false,
        evaluation: null,
      },
      servedFromCache: false,
    },
  };
}

function send(res, status, body) {
  const json = JSON.stringify(body);
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  });
  res.end(json);
}

const server = http.createServer((req, res) => {
  if (req.method === "OPTIONS") {
    send(res, 204, {});
    return;
  }

  const url = new URL(req.url, `http://localhost:${PORT}`);

  if (req.method === "GET" && url.pathname === "/dev/token") {
    const customerId = url.searchParams.get("customerId") || "CUST1001";
    const role = url.searchParams.get("role") || "CUSTOMER";
    send(res, 200, { token: `MOCK.${customerId}.${role}`, warning: "MOCK token -- not a real JWT, see server.js header comment." });
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/v1/admin/audit-log/tail") {
    const claims = decodeMockToken(req.headers["authorization"]);
    if (!claims || claims.role !== "ADMIN") {
      send(res, 403, { error: "Forbidden -- ADMIN role required (mock @PreAuthorize equivalent)" });
      return;
    }
    send(res, 200, { totalEvents: 0, lines: ["(mock server does not persist an audit log -- run the real service for reports/audit-log.jsonl)"] });
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/v1/support/ask") {
    let body = "";
    req.on("data", (chunk) => (body += chunk));
    req.on("end", () => {
      let parsed;
      try {
        parsed = JSON.parse(body || "{}");
      } catch {
        send(res, 400, { error: "Malformed JSON body" });
        return;
      }
      const { query, requestedCustomerId } = parsed;
      if (!query || !query.trim()) {
        send(res, 400, { error: "query is required" });
        return;
      }

      const intent = classify(query);
      if (intent === "POLICY_QUESTION") {
        send(res, 200, policyAnswer(query));
        return;
      }
      if (intent === "AMBIGUOUS") {
        send(res, 200, { type: "AMBIGUOUS", payload: { message: "This question seems to combine more than one type of request. Please ask about your account balance, transactions, loan outstanding, or a policy question separately." } });
        return;
      }

      const claims = decodeMockToken(req.headers["authorization"]);
      if (!claims) {
        send(res, 200, { type: "ACCESS_DENIED", payload: { reason: "Authentication failed: Token is missing" } });
        return;
      }
      const access = evaluateAccess(claims.subject, claims.role, requestedCustomerId);
      if (!access.allowed) {
        send(res, 200, { type: "ACCESS_DENIED", payload: { reason: access.reason } });
        return;
      }

      const dataByIntent = {
        ACCOUNT_BALANCE: ACCOUNTS[requestedCustomerId] || [],
        TRANSACTION_HISTORY: TRANSACTIONS[requestedCustomerId] || [],
        LOAN_OUTSTANDING: LOANS[requestedCustomerId] || [],
      };
      send(res, 200, { type: "LIVE_DATA", payload: { intent, data: dataByIntent[intent] } });
    });
    return;
  }

  send(res, 404, { error: "Not found (mock server only implements /api/v1/support/ask, /dev/token, /api/v1/admin/audit-log/tail)" });
});

server.listen(PORT, () => {
  console.log(`Mock banking-support-service listening on http://localhost:${PORT}`);
  console.log("This is NOT the real backend -- see this file's header comment.");
});
