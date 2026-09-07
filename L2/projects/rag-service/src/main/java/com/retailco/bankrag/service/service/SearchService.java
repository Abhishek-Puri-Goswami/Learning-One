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
 * Contains the business logic for search, including a "weak retrieval"
 * guardrail, kept separate from the HTTP controller. It lets a caller
 * pick a search strategy through one unified method, and flags
 * low-confidence results instead of silently returning them as if they
 * were reliable.
 * <p>
 * How the guardrail works (see {@code search()} below): after getting
 * ranked results, it checks the top score against a minimum threshold
 * AND checks the gap between the top and second result — is the top
 * result CLEARLY the best match, or just barely ahead of an unrelated
 * runner-up? Either check failing flags the result as low-confidence.
 * This guardrail only applies to SEMANTIC and HYBRID search, since
 * keyword-search scores measure something different (literal word
 * overlap) and aren't the kind of signal this check is designed to
 * catch.
 * <p>
 * The {@code @Value} fields below pull their default thresholds from
 * configuration, which makes them tunable per deployment through
 * {@code application.yml} or environment variables — no code change or
 * redeploy needed to adjust them.
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

        // This guardrail only meaningfully applies to semantic/hybrid
        // scores — keyword scores are literal term-overlap fractions, a
        // different kind of measurement this check isn't designed for.
        if (method != Method.KEYWORD) {
            if (results.isEmpty() || topScore < similarityThreshold) {
                guardrailTriggered = true;
                guardrailMessage = "No sufficiently relevant policy content was found for this query "
                        + "(top score below the " + similarityThreshold + " similarity threshold). "
                        + "This question may be outside the scope of the ingested policy corpus.";
            } else if (scoreMargin != null && scoreMargin < minScoreMargin) {
                // A top score that clears the threshold but is barely
                // ahead of the runner-up is a weaker signal than the raw
                // number alone suggests — flag it too.
                guardrailTriggered = true;
                guardrailMessage = "Top result score is too close to the next-best result "
                        + "(margin " + String.format("%.3f", scoreMargin) + " < " + minScoreMargin
                        + "). Treat this retrieval as low-confidence.";
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
