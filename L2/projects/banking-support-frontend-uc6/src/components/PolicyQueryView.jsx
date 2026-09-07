import { useState } from "react";
import { askSupport, ApiError } from "../api";

/**
 * Deliverable 8.2 "Policy query UI." Sends a query with no Authorization
 * header and no customer id -- exactly how a POLICY_QUESTION intent is
 * meant to be asked (see IntegratedBankingAssistant's Javadoc: policy
 * questions never require a token). Renders the grounded answer plus its
 * citations, or the guardrail-block reason, or the cache-hit flag -- every
 * field RagAssistant.AssistantResponse actually returns, not a trimmed-down
 * subset.
 */
export default function PolicyQueryView() {
  const [query, setQuery] = useState("What are home loan eligibility rules?");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const response = await askSupport({ query });
      setResult(response);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not reach the banking support service.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="panel">
      <h2>Policy question</h2>
      <p className="hint">
        Ask about loan eligibility, KYC, card issuance, or complaint escalation. No login required --
        policy questions are answered from Secure Bank's public policy manual.
      </p>
      <form onSubmit={handleSubmit} className="query-form">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="e.g. What documents are required for KYC verification?"
        />
        <button type="submit" disabled={loading}>
          {loading ? "Asking..." : "Ask"}
        </button>
      </form>

      {error && <div className="error-box">{error}</div>}

      {result && result.type === "POLICY_ANSWER" && (() => {
        const p = result.payload;
        const rag = p.ragResponse;
        if (rag.blocked) {
          return <div className="blocked-box">Blocked by guardrail: {rag.blockReason}</div>;
        }
        return (
          <div className="answer-box">
            {p.servedFromCache && <span className="badge">served from cache</span>}
            {rag.fallback && <span className="badge badge-warn">low-confidence fallback</span>}
            <p className="answer-text">{rag.answer}</p>
            {rag.citations && rag.citations.length > 0 && (
              <div className="citations">
                <strong>Citations</strong>
                <ul>
                  {rag.citations.map((c, i) => (
                    <li key={i}>
                      {c.sourceDocument} #{c.chunkIndex}
                      {typeof c.similarityScore === "number" && ` (score ${c.similarityScore.toFixed(3)})`}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        );
      })()}

      {result && result.type === "AMBIGUOUS" && (
        <div className="ambiguous-box">{result.payload.message}</div>
      )}
    </div>
  );
}
