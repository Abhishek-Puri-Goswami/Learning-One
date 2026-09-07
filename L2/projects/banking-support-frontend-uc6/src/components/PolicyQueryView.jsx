import { useState } from "react";
import { askSupport, ApiError } from "../api";

/**
 * The "Policy Q&A" tab: a simple text box where the user can ask a
 * general question, like "What documents are needed for KYC
 * verification?" Notice this sends the question with no login token and
 * no customer id at all — a policy question never needs one, since it's
 * answered from public policy documents, not from anyone's personal
 * account data.
 * <p>
 * The response can show up a few different ways: a normal answer with
 * its supporting citations, a "served from cache" badge if this exact
 * question was already answered recently, a warning badge if the answer
 * is a low-confidence guess, or a "blocked" message if a safety
 * guardrail decided the question shouldn't be answered at all.
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
        Ask about loan eligibility, KYC, card issuance, or complaint escalation. No login required —
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
