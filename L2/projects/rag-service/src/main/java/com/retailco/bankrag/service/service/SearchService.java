package com.retailco.bankrag.service.service;

import com.retailco.bankrag.core.HybridSearcher;
import com.retailco.bankrag.core.KeywordSearcher;
import com.retailco.bankrag.core.ScoredChunk;
import com.retailco.bankrag.core.VectorStore;
import com.retailco.bankrag.service.dto.SearchResponse;
import com.retailco.bankrag.service.dto.SearchResultItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

// CONCEPT: Service layer -- business logic for retrieval, including a
// "weak retrieval" guardrail, kept separate from the HTTP controller.
// PURPOSE: Lets a caller pick a search strategy (KEYWORD/SEMANTIC/HYBRID)
// via one unified method, and applies a guardrail on top of the raw
// scores so low-confidence results are flagged rather than silently
// returned as if they were reliable.
// HOW THE GUARDRAIL WORKS (see search() below): after getting ranked
// results, check the top score against `similarityThreshold` (an absolute
// floor) AND check the gap between the top and second result against
// `minScoreMargin` (is the top result CLEARLY the best, or just barely
// ahead of an unrelated runner-up?). Either failing sets
// `guardrailTriggered=true` with an explanatory message -- the guardrail
// only applies to SEMANTIC/HYBRID methods, since KEYWORD scores are literal
// term-overlap fractions, not the kind of relevance signal this check is
// designed to catch.
// WHY @Value fields with defaults (e.g. "${bankrag.search.similarity-threshold:0.15}"):
// this makes the thresholds tunable per-deployment via
// application.yml/environment variables, without a code change or
// redeploy for a new value.
@Service
public class SearchService {

    public enum Method { KEYWORD, SEMANTIC, HYBRID }

    private final VectorStore vectorStore;
    private final KeywordSearcher keywordSearcher = new KeywordSearcher();

    @Value("${bankrag.search.similarity-threshold:0.15}")
    private double similarityThreshold;

    @Value("${bankrag.search.min-score-margin:0.03}")
    private double minScoreMargin;

    @Value("${bankrag.search.semantic-weight:0.6}")
    private double semanticWeight;

    @Value("${bankrag.search.keyword-weight:0.4}")
    private double keywordWeight;

    public SearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public SearchResponse search(String query, Method method, int topK) {
        List<ScoredChunk> results = switch (method) {
            case KEYWORD -> keywordSearcher.search(vectorStore.allChunks(), query, topK);
            case SEMANTIC -> vectorStore.semanticSearch(query, topK, 0.0); // guardrail applied below, not inside rag-core
            case HYBRID -> new HybridSearcher(vectorStore, keywordSearcher, semanticWeight, keywordWeight)
                    .search(query, topK, 0.0);
        };

        boolean guardrailTriggered = false;
        String guardrailMessage = null;
        Double topScore = results.isEmpty() ? null : results.get(0).score();
        Double scoreMargin = results.size() >= 2
                ? results.get(0).score() - results.get(1).score()
                : null;

        // Guardrail only meaningfully applies to semantic/hybrid scores, which
        // are the ones discussed in hallucination-risk-analysis.md -- keyword
        // scores are literal term-overlap fractions, not a relevance signal
        // this guardrail was designed to gate.
        if (method != Method.KEYWORD) {
            if (results.isEmpty() || topScore < similarityThreshold) {
                guardrailTriggered = true;
                guardrailMessage = "No sufficiently relevant policy content was found for this query "
                        + "(top score below the " + similarityThreshold + " similarity threshold). "
                        + "This question may be outside the scope of the ingested policy corpus.";
            } else if (scoreMargin != null && scoreMargin < minScoreMargin) {
                // Score-margin check per hallucination-risk-analysis.md
                // Recommendation 2: a top score that clears the absolute
                // threshold but is barely ahead of the runner-up is a weaker
                // signal than the raw number alone suggests.
                guardrailTriggered = true;
                guardrailMessage = "Top result score is too close to the next-best result "
                        + "(margin " + String.format("%.3f", scoreMargin) + " < " + minScoreMargin
                        + "). Treat this retrieval as low-confidence -- see "
                        + "reports/hallucination-risk-analysis.md.";
            }
        }

        List<SearchResultItem> items = results.stream()
                .map(sc -> new SearchResultItem(
                        sc.chunk().id(), sc.chunk().sourceDocument(),
                        sc.chunk().chunkIndex(), sc.chunk().text(), sc.score()))
                .toList();

        return new SearchResponse(query, method.name(), items,
                guardrailTriggered, guardrailMessage, topScore, scoreMargin);
    }
}
