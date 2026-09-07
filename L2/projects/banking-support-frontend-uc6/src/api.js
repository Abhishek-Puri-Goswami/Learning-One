/**
 * This is the one place where the frontend talks to the backend.
 * {@code askSupport} sends a question to a single endpoint,
 * {@code POST /api/v1/support/ask}, and gets back a response shaped like
 * {@code { type, payload }}, where {@code type} is one of
 * {@code "POLICY_ANSWER"}, {@code "LIVE_DATA"}, {@code "ACCESS_DENIED"},
 * or {@code "AMBIGUOUS"}.
 * <p>
 * Notice there's no logic here deciding whether a question is a policy
 * question or a live banking-data question — that decision is made once,
 * on the server, before this frontend ever sees the response. This
 * file's only job is to send the question and hand back whatever comes
 * back, so the component using it can decide how to display each
 * {@code type}.
 */

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
