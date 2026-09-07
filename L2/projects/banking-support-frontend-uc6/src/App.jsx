import { useState } from "react";
import PolicyQueryView from "./components/PolicyQueryView";
import BankingDashboardView from "./components/BankingDashboardView";
import "./index.css";

/**
 * Deliverable: "Frontend Interface" (L2 HLD UseCase6, 8.2): "Policy query
 * UI" and "Banking dashboard (balance/cards/loans)." Both views talk to the
 * same single /api/v1/support/ask endpoint (see api.js) -- there is no
 * separate frontend route per backend intent, matching the backend's own
 * "one unified entry point" design (IntegratedBankingAssistant).
 */
export default function App() {
  const [tab, setTab] = useState("policy");

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">Secure Bank <span>AI Support</span></div>
        <nav className="tabs">
          <button className={tab === "policy" ? "active" : ""} onClick={() => setTab("policy")}>
            Policy Q&A
          </button>
          <button className={tab === "dashboard" ? "active" : ""} onClick={() => setTab("dashboard")}>
            My Account
          </button>
        </nav>
      </header>
      <main className="app-main">
        {tab === "policy" ? <PolicyQueryView /> : <BankingDashboardView />}
      </main>
      <footer className="app-footer">
        L2 / UC6 — Final Integrated Banking RAG System (with CI/CD). Talks to banking-support-service at{" "}
        {import.meta.env.VITE_API_BASE_URL || "http://localhost:8085"}.
      </footer>
    </div>
  );
}
