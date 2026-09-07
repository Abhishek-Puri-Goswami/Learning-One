package com.retailco.bankrag.assistant;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A guardrail that blocks questions asking for personalized financial,
 * investment, or legal advice — like "should I invest in mutual funds
 * right now?" A policy-document assistant should never answer those,
 * REGARDLESS of what the document search happens to find.
 * <p>
 * This is different from the retrieval guardrail (which blocks based on a
 * weak similarity score). This one blocks based on what KIND of question
 * was asked, independent of what got retrieved — because a compliance
 * document might happen to mention "mutual funds" in an unrelated
 * paragraph, and a purely similarity-based check alone could let a
 * misleading, advice-shaped answer slip through.
 * <p>
 * It works the same way {@code PromptInjectionGuard} does: match the
 * question against a fixed list of advice-seeking phrases, and block on
 * the first match. This acts as an earlier, second layer of defense on
 * top of the prompt itself also telling the AI not to give advice — so
 * the block still works even if the AI ignored that instruction.
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
