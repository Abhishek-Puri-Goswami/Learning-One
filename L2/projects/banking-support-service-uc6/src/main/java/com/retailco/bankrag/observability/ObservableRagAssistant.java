package com.retailco.bankrag.observability;

import com.retailco.bankrag.assistant.RagAssistant;

/**
 * Wraps L2/UC2's RagAssistant with the three L2 UC4 concerns that need to
 * sit AROUND every call, not inside retrieval/generation itself: caching
 * (check before calling, store after), metrics recording (latency + token
 * usage, already present on AssistantResponse.evaluation() -- this class
 * doesn't recompute them, it just captures them), and cost estimation.
 *
 * This is intentionally a decorator around RagAssistant, not a
 * modification of it -- L2/UC2's pipeline (guardrails -> retrieval ->
 * prompt -> generation -> citation -> trace -> evaluation) is unchanged
 * and still independently correct; UC4 adds an observability layer on top
 * without touching UC2's already-verified internals.
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
            // Cache hit: zero additional latency, zero additional tokens,
            // zero additional cost -- record that explicitly rather than
            // re-recording the original call's numbers, since the whole
            // point of this metric is to show the SAVINGS.
            metricsRecorder.record(new MetricsRecorder.QueryMetric(
                    query, true, cached.blocked(), cached.fallback(), 0, 0, 0.0));
            return new ObservedResponse(cached, true);
        }

        long start = System.currentTimeMillis();
        RagAssistant.AssistantResponse response = ragAssistant.ask(query);
        long wallClockMs = System.currentTimeMillis() - start;

        int totalTokens = response.evaluation().totalTokens();
        // Evaluation only carries the combined total; for cost estimation
        // purposes here, prompt/completion are split 80/20 as a reasonable
        // approximation for this pipeline's shape (a long retrieved-context
        // prompt, a short extractive answer) -- disclosed as an
        // approximation in cost/cost-estimation-document.md, not presented
        // as an exact per-call breakdown.
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
