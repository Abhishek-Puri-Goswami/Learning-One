package com.retailco.bankrag.assistant;

import com.retailco.bankrag.core.ScoredChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// CONCEPT: Post-processing / response parsing -- turning an LLM's raw text
// output back into structured data.
// PURPOSE: PromptTemplate instructs the LLM to cite sources inline as
// "[chunk-id]" markers (e.g. "[loan_processing_policy.txt#2]"). This class
// finds every such marker in the generated answer and resolves it back to
// its full Citation (source document, chunk index, similarity score), so a
// caller like a REST API/frontend gets structured citation data instead of
// having to re-parse the answer text itself.
//
// HOW IT WORKS: a regex (CITATION_PATTERN) finds every "[...]" bracket
// that looks like a chunk id, de-duplicates repeats of the same id, then
// looks each one up in the map of chunks that were ACTUALLY retrieved for
// this query.
//
// IMPORTANT (hallucinated-citation detection): if a citation's id is NOT
// found in that map, it's flagged with `resolvable=false` rather than
// silently dropped. A real generative LLM can occasionally invent a
// plausible-looking citation to a chunk that was never actually retrieved
// ("hallucinated citation") -- surfacing that instead of hiding it lets
// callers detect when the LLM cited something it shouldn't have.
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
