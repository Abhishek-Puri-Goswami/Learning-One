package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.ScoredChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The AI is instructed to cite its sources inline in its answer, like
 * {@code "...minimum income is Rs.20,000 [loan_processing_policy.txt#2]."}.
 * This class finds every one of those bracketed citations in the
 * generated answer and turns each one into a proper, structured
 * {@code Citation} object — so whoever displays the answer (a web page,
 * for example) gets clean citation data instead of having to hunt through
 * the raw text itself.
 * <p>
 * How it works: a pattern search finds every bracketed reference that
 * looks like a chunk id, skips any repeats of the same citation, then
 * looks each one up against the chunks that were ACTUALLY retrieved for
 * this question.
 * <p>
 * One important safety check: if a citation doesn't match any chunk we
 * actually retrieved, it's marked {@code resolvable=false} instead of
 * being silently thrown away. Every so often, an AI model can "invent" a
 * citation that sounds plausible but doesn't correspond to anything real
 * — this is sometimes called a "hallucination." Flagging it clearly, like
 * we do here, lets whoever's reading the answer notice when that's
 * happened, rather than hiding the problem.
 */
public final class CitationExtractor {

    public record Citation(String chunkId, String sourceDocument, int chunkIndex,
                            double similarityScore, boolean resolvable) {
    }

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[([^\\[\\]]+#\\d+)]");

    private CitationExtractor() {
    }

    public static List<Citation> extract(String answerText, List<ScoredChunk> retrievedChunks) {
        Map<String, ScoredChunk> byId = new LinkedHashMap<>();
        for (ScoredChunk sc : retrievedChunks) {
            byId.put(sc.chunk().id(), sc);
        }

        List<Citation> citations = new ArrayList<>();
        Matcher matcher = CITATION_PATTERN.matcher(answerText);
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            String chunkId = matcher.group(1);
            if (!seen.add(chunkId)) continue; // de-duplicate repeated citations of the same chunk
            ScoredChunk sc = byId.get(chunkId);
            if (sc != null) {
                citations.add(new Citation(chunkId, sc.chunk().sourceDocument(),
                        sc.chunk().chunkIndex(), sc.score(), true));
            } else {
                // Unresolvable citation -- would indicate a hallucinated source
                // reference; flagged rather than silently dropped, per
                // reports/hallucination-risk-analysis.md from UC1's guardrail
                // philosophy of surfacing weak signals instead of hiding them.
                citations.add(new Citation(chunkId, "UNKNOWN", -1, 0.0, false));
            }
        }
        return citations;
    }
}
