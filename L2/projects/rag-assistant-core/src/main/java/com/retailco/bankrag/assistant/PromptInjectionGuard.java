package com.retailco.bankrag.assistant;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Deliverable: "Guardrail and fallback logic" (prompt-injection half).
 * L2 HLD UseCase2 functional scope: "Prompt-injection protection" /
 * "Block unsafe or injected prompts."
 *
 * A real production system would pair this heuristic layer with an
 * LLM-based classifier and/or a managed guardrail service (e.g. AWS
 * Bedrock Guardrails, Azure AI Content Safety, NeMo Guardrails) -- not
 * reachable from this sandbox (no network egress, no API keys). This is
 * the deterministic, always-available first line of defense: pattern
 * matching against known prompt-injection phrasings, run BEFORE the query
 * ever reaches retrieval or the LLM, so a blocked query costs nothing.
 */
public final class PromptInjectionGuard {

    public record Verdict(boolean blocked, String reason) {
        static Verdict allow() {
            return new Verdict(false, null);
        }

        static Verdict block(String reason) {
            return new Verdict(true, reason);
        }
    }

    // Each pattern targets a well-documented injection family (OWASP LLM01):
    // instruction override, role/persona hijack, system-prompt exfiltration,
    // and delimiter/escape attempts.
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore (all |any |the |previous |prior |above )+instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard (all |any |the |previous |prior |above )+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(reveal|print|show|repeat|output) (your|the) (system prompt|instructions|prompt)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now|act as|pretend (to be|you are)|from now on you", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget (everything|all|your instructions)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDAN\\b|developer mode|jailbreak", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?(system|assistant|context)>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("override (your|the) (rules|guardrails|restrictions)", Pattern.CASE_INSENSITIVE)
    );

    public Verdict check(String userQuery) {
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(userQuery).find()) {
                return Verdict.block("Query matched prompt-injection pattern: " + p.pattern());
            }
        }
        return Verdict.allow();
    }
}
