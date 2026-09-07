package com.retailco.bankrag.assistant;

import java.util.List;
import java.util.regex.Pattern;

// CONCEPT: Guardrail layer -- a scope guardrail (as opposed to
// PromptInjectionGuard's security guardrail).
// PURPOSE: Blocks questions asking for personalized financial/investment/
// legal advice (e.g. "should I invest in mutual funds right now?"),
// because a policy-document assistant should never answer those --
// REGARDLESS of what the retriever happens to find. This is different
// from a low-similarity-score guardrail: even if retrieval accidentally
// returns a document that LOOKS related, this guard still blocks the
// query, because the question TYPE itself is out of scope.
//
// HOW IT WORKS: same technique as PromptInjectionGuard -- match the raw
// query against a fixed list of regex patterns for advice-seeking phrasing
// ("should I invest...", "best mutual fund...", "is it a good time to
// invest...") and return a blocking Verdict on the first match.
//
// WHY this exists as a SEPARATE guard from the similarity threshold: a
// policy document might mention "mutual funds" in an unrelated compliance
// paragraph, so a purely similarity-based check could let this kind of
// question slip through with a misleadingly plausible-looking answer.
// Blocking by query INTENT, independent of retrieval, closes that gap
// (defense in depth: the prompt also tells the LLM not to give advice --
// see PromptTemplate rule 1 -- so this is a second, earlier layer that
// works even if the LLM ignored its instructions).
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
