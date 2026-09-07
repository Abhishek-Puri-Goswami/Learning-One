package com.retailco.bankrag.assistant;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Deliverable: "Guardrail and fallback logic" (unsupported-financial-advice
 * half). L2 HLD UseCase1 System Responsibilities carries this forward too:
 * "Avoid unsupported financial advice" -- UC2 operationalizes it as an
 * explicit pre-generation guardrail rather than relying only on the prompt's
 * instructions (defense in depth: the prompt tells the LLM not to give
 * advice; this guard stops the query before generation even if the LLM
 * were to ignore that instruction).
 *
 * Distinguishes "asking what the bank's policy says" (answerable from
 * retrieved documents, allowed) from "asking the assistant to give
 * personal financial/investment advice" (never answerable from a policy
 * manual, blocked regardless of retrieval results) -- the same distinction
 * exercised by UC1's demo query set (a legitimate FD-withdrawal-policy
 * question vs. a "should I invest in mutual funds" question -- see
 * L2/UC1/reports/hallucination-risk-analysis.md, whose finding motivates
 * having this guard run independently of the similarity-score guardrail).
 */
public final class UnsafeQueryGuard {

    public record Verdict(boolean blocked, String reason) {
        static Verdict allow() {
            return new Verdict(false, null);
        }

        static Verdict block(String reason) {
            return new Verdict(true, reason);
        }
    }

    private static final List<Pattern> ADVICE_SEEKING_PATTERNS = List.of(
            Pattern.compile("should i (invest|buy|sell|put my money)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(best|good|guaranteed) (stock|mutual fund|investment|returns)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("which (stock|fund|crypto|cryptocurrency) should i", Pattern.CASE_INSENSITIVE),
            Pattern.compile("is it (a good time|worth it) to invest", Pattern.CASE_INSENSITIVE),
            Pattern.compile("financial advice|investment advice|legal advice", Pattern.CASE_INSENSITIVE),
            Pattern.compile("guarantee(d)? (returns|profit)", Pattern.CASE_INSENSITIVE)
    );

    public Verdict check(String userQuery) {
        for (Pattern p : ADVICE_SEEKING_PATTERNS) {
            if (p.matcher(userQuery).find()) {
                return Verdict.block("Query requests personalized financial/investment/legal advice, "
                        + "which is outside a policy-document assistant's scope regardless of what "
                        + "retrieval returns.");
            }
        }
        return Verdict.allow();
    }
}
