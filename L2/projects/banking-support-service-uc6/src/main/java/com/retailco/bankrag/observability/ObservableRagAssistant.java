package com.retailco.bankrag.observability;

import com.retailco.bankrag.assistant.RagAssistant;

/**
 * This class wraps {@code RagAssistant} to add caching, metrics, and cost
 * tracking around it — without changing a single line of
 * {@code RagAssistant}'s own code. This design idea is called the
 * "Decorator pattern": you wrap an existing object to add new behavior
 * around it, while leaving the original untouched.
 * <p>
 * Here's what {@code ask()} does, step by step:
 * <ol>
 *   <li>Check the cache first. If the answer's already there, record a
 *       "free" metric (zero latency, zero cost — explicitly showing the
 *       SAVINGS caching gives us) and return right away.
 *       {@code ragAssistant.ask()} never even gets called.</li>
 *   <li>On a cache miss, call the real {@code ragAssistant.ask()}, time
 *       how long it takes, estimate its cost from its token count, and
 *       record a full metric entry.</li>
 *   <li>Save the fresh answer in the cache for next time.</li>
 * </ol>
 * <p>
 * Why wrap {@code RagAssistant} instead of changing it directly:
 * {@code RagAssistant} is already correct and tested on its own. Wrapping
 * it means that logic stays completely untouched and still independently
 * verifiable — this class only ever adds behavior AROUND it.
 */
public class ObservableRagAssistant {

    private final RagAssistant ragAssistant;
    private final QueryCache cache;
    private final MetricsRecorder metricsRecorder;
    private final CostEstimator costEstimator;

    public ObservableRagAssistant(RagAssistant ragAssistant, QueryCache cache,
                                   MetricsRecorder metricsRecorder, CostEstimator costEstimator) {
        this.ragAssistant = ragAssistant;
        this.cache = cache;
        this.metricsRecorder = metricsRecorder;
        this.costEstimator = costEstimator;
    }

    public record ObservedResponse(RagAssistant.AssistantResponse response, boolean servedFromCache) {
    }

    public ObservedResponse ask(String query) {
        RagAssistant.AssistantResponse cached = cache.get(query);
        if (cached != null) {
            // A cache hit means zero extra latency, tokens, and cost — we
            // record that explicitly (rather than reusing the original
            // call's numbers), because the whole point of this metric is
            // to show how much caching actually saved us.
            metricsRecorder.record(new MetricsRecorder.QueryMetric(
                    query, true, cached.blocked(), cached.fallback(), 0, 0, 0.0));
            return new ObservedResponse(cached, true);
        }

        long start = System.currentTimeMillis();
        RagAssistant.AssistantResponse response = ragAssistant.ask(query);
        long wallClockMs = System.currentTimeMillis() - start;

        int totalTokens = response.evaluation().totalTokens();
        // We only have the COMBINED token total available here, not a
        // separate prompt/completion split. Since a typical call in this
        // pipeline involves a long prompt (with retrieved context) and a
        // much shorter answer, we approximate an 80/20 split — a
        // reasonable estimate, not an exact per-call breakdown.
        int approxPromptTokens = (int) Math.round(totalTokens * 0.8);
        int approxCompletionTokens = totalTokens - approxPromptTokens;
        double cost = costEstimator.estimateCostUsd(approxPromptTokens, approxCompletionTokens).doubleValue();

        metricsRecorder.record(new MetricsRecorder.QueryMetric(
                query, false, response.blocked(), response.fallback(),
                response.evaluation().latencyMs(), totalTokens, cost));

        cache.put(query, response);
        return new ObservedResponse(response, false);
    }

    public QueryCache.CacheStats cacheStats() {
        return cache.stats();
    }

    public MetricsRecorder.AggregateReport metricsReport() {
        return metricsRecorder.aggregate();
    }
}
