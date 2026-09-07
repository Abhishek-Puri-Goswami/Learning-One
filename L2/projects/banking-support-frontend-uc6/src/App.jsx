import { useState } from "react";
import PolicyQueryView from "./components/PolicyQueryView";
import BankingDashboardView from "./components/BankingDashboardView";
import "./index.css";

/**
 * The root component of this banking support app. It shows two tabs —
 * "Policy Q&A" for asking general questions, and "My Account" for
 * viewing balances, transactions, and loans — and switches between them
 * with a bit of local state ({@code tab}).
 * <p>
 * Even though these look like two separate features, both tabs actually
 * send their questions to the exact same backend endpoint (see
 * {@code api.js}). The backend itself figures out whether a question is
 * a policy question or a live-data question — the frontend doesn't need
 * a separate route or endpoint for each.
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
