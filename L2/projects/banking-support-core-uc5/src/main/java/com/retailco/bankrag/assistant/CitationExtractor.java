package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.ScoredChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deliverable: "Citation-enabled responses."
 *
 * Parses the inline [chunk-id] markers PromptTemplate's rules require the
 * LLM to emit (see SYSTEM_INSTRUCTIONS rule 2) out of the raw generated
 * text, and resolves each one back to its full source chunk (document,
 * chunk index, similarity score) from the chunks that were actually
 * retrieved for this query -- so a caller (e.g. the React frontend) can
 * render clickable/expandable citations rather than parsing raw text
 * itself.
 *
 * Also flags citations that reference a chunk id NOT present in the
 * retrieved set -- a real generative LLM can occasionally invent a
 * plausible-looking citation ("hallucinated citation"); this check would
 * catch that even though the deterministic ExtractiveStubLlmClient never
 * actually produces one (it only ever cites chunk ids it was given).
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
