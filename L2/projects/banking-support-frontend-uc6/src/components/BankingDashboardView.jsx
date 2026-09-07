import { useState } from "react";
import { askSupport, fetchDevToken, ApiError } from "../api";

// These plain-English phrasings are what the backend recognizes as
// "show me live banking data" questions. Even though this dashboard
// looks like a set of dedicated buttons, under the hood each one sends a
// normal question — the SAME endpoint the Policy Q&A tab uses — just
// with a login token and customer id attached this time, since these
// questions need to know who's asking.
const ACTIONS = [
  { key: "balance", label: "Account balance", query: "What is my account balance?" },
  { key: "transactions", label: "Recent transactions", query: "Show me my recent transactions" },
  { key: "loan", label: "Loan outstanding", query: "How much is left on my loan?" },
];

/**
 * The "My Account" tab: a small dashboard of buttons for checking
 * account balance, recent transactions, and loan details — all backed
 * by real (masked) data, never anything generated or guessed by an AI
 * model.
 * <p>
 * Using any of these requires a login token. In this demo, a "Get dev
 * token" button can generate one instantly for testing — a real app
 * would replace that with an actual sign-in screen. A token with the
 * SUPPORT_AGENT or ADMIN role can look up another customer's data;
 * a plain CUSTOMER token can only see its own.
 */
export default function BankingDashboardView() {
  const [customerId, setCustomerId] = useState("CUST1001");
  const [role, setRole] = useState("CUSTOMER");
  const [token, setToken] = useState("");
  const [accountNumber, setAccountNumber] = useState("100200300456");
  const [results, setResults] = useState({});
  const [errors, setErrors] = useState({});
  const [loadingKey, setLoadingKey] = useState(null);
  const [tokenError, setTokenError] = useState(null);

  async function handleGetDevToken() {
    setTokenError(null);
    try {
      const res = await fetchDevToken({ customerId, role });
      setToken(res.token);
    } catch (err) {
      setTokenError(
        err instanceof ApiError
          ? `${err.message} (the /dev/token endpoint only exists when the backend is running in its "dev" mode)`
          : "Could not reach the banking support service."
      );
    }
  }

  async function runAction(action) {
    setLoadingKey(action.key);
    setErrors((e) => ({ ...e, [action.key]: null }));
    try {
      const response = await askSupport({
        query: action.query,
        token,
        requestedCustomerId: customerId,
        accountNumber: action.key === "transactions" ? accountNumber : undefined,
      });
      setResults((r) => ({ ...r, [action.key]: response }));
    } catch (err) {
      setErrors((e) => ({
        ...e,
        [action.key]: err instanceof ApiError ? err.message : "Could not reach the banking support service.",
      }));
    } finally {
      setLoadingKey(null);
    }
  }

  return (
    <div className="panel">
      <h2>Banking dashboard</h2>
      <p className="hint">
        Requires a login token. Get a dev token below (dev/demo builds only) or
        paste a real one. A SUPPORT_AGENT or ADMIN token can view another customer's data;
        a plain CUSTOMER token only its own.
      </p>

      <div className="token-row">
        <input value={customerId} onChange={(e) => setCustomerId(e.target.value)} placeholder="Customer ID (e.g. CUST1001)" />
        <select value={role} onChange={(e) => setRole(e.target.value)}>
          <option value="CUSTOMER">CUSTOMER</option>
          <option value="SUPPORT_AGENT">SUPPORT_AGENT</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <button type="button" onClick={handleGetDevToken}>Get dev token</button>
      </div>
      <textarea
        className="token-box"
        value={token}
        onChange={(e) => setToken(e.target.value)}
        placeholder="JWT bearer token"
        rows={2}
      />
      {tokenError && <div className="error-box">{tokenError}</div>}

      <div className="account-row">
        <label>
          Account number (for transaction history)
          <input value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} />
        </label>
      </div>

      <div className="action-grid">
        {ACTIONS.map((action) => (
          <div key={action.key} className="action-card">
            <div className="action-card-header">
              <span>{action.label}</span>
              <button type="button" onClick={() => runAction(action)} disabled={!token || loadingKey === action.key}>
                {loadingKey === action.key ? "Loading..." : "Fetch"}
              </button>
            </div>
            {errors[action.key] && <div className="error-box">{errors[action.key]}</div>}
            {results[action.key] && <ResultView response={results[action.key]} />}
          </div>
        ))}
      </div>
    </div>
  );
}

// Renders whatever came back from an action button: an "access denied"
// message, a table of real data rows, or a raw JSON dump as a fallback
// for any response shape not specifically handled above.
function ResultView({ response }) {
  if (response.type === "ACCESS_DENIED") {
    return <div className="blocked-box">Access denied: {response.payload.reason}</div>;
  }
  if (response.type === "LIVE_DATA") {
    const items = response.payload.data;
    if (!Array.isArray(items) || items.length === 0) {
      return <div className="empty-box">No records found.</div>;
    }
    return (
      <table className="data-table">
        <thead>
          <tr>
            {Object.keys(items[0]).map((k) => (
              <th key={k}>{k}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {items.map((item, i) => (
            <tr key={i}>
              {Object.values(item).map((v, j) => (
                <td key={j}>{String(v)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    );
  }
  return <pre className="raw-json">{JSON.stringify(response, null, 2)}</pre>;
}
