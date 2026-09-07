// Deliverable: "Frontend Interface" (L2 HLD UseCase6, 8.2). Talks to
// exactly one endpoint -- POST /api/v1/support/ask -- matching
// SupportController.java's contract byte-for-byte:
//   request:  { query, requestedCustomerId?, accountNumber? }
//   response: { type: "POLICY_ANSWER" | "LIVE_DATA" | "ACCESS_DENIED" | "AMBIGUOUS", payload }
// No client-side routing logic decides POLICY vs LIVE_DATA -- that
// decision is made once, server-side, by IntentClassifier (L2/UC3). The
// frontend's only job is to render whichever `type` comes back.

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8085";

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

export async function askSupport({ query, token, requestedCustomerId, accountNumber }) {
  const headers = { "Content-Type": "application/json" };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}/api/v1/support/ask`, {
    method: "POST",
    headers,
    body: JSON.stringify({ query, requestedCustomerId, accountNumber }),
  });

  if (!res.ok) {
    throw new ApiError(`Request failed with status ${res.status}`, res.status);
  }
  return res.json();
}

export async function fetchDevToken({ customerId, role }) {
  const res = await fetch(`${BASE_URL}/dev/token?customerId=${encodeURIComponent(customerId)}&role=${encodeURIComponent(role)}`);
  if (!res.ok) {
    throw new ApiError(`Dev token request failed with status ${res.status}`, res.status);
  }
  return res.json();
}

export async function fetchAuditTail({ token, lines = 20 }) {
  const res = await fetch(`${BASE_URL}/api/v1/admin/audit-log/tail?lines=${lines}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    throw new ApiError(`Audit log request failed with status ${res.status}`, res.status);
  }
  return res.json();
}
