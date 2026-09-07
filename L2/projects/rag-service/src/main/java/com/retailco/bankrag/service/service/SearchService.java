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

/**
 * Wraps rag-core's KeywordSearcher / VectorStore.semanticSearch / HybridSearcher
 * behind a single REST-callable "search" operation with a selectable method,
 * implementing the "retrieval comparison" functional requirement (see
 * reports/retrieval-comparison-summary.md, which was produced by exercising
 * this exact logic via rag-core's Main.java CLI demo before this REST wrapper
 * existed) as a callable API rather than only a one-off CLI run.
 *
 * Also implements the hallucination-risk guardrail from
 * reports/hallucination-risk-analysis.md: both an absolute similarity
 * threshold AND a score-margin check (Recommendation 2 of that report) are
 * applied here, since the analysis concluded that a single fixed threshold
 * is not sufficient on its own.
 */
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
