package com.retailco.bankrag.observability;

import com.retailco.bankrag.assistant.RagAssistant;

// CONCEPT: Decorator pattern -- wraps an existing object (RagAssistant)
// to add new behavior (caching, metrics, cost tracking) around it, without
// modifying the wrapped object's own code.
// PURPOSE: RagAssistant already does guardrails -> retrieval -> generation
// -> citation -> evaluation -> tracing correctly on its own. This class
// adds three cross-cutting observability concerns AROUND every call, in
// one place, so RagAssistant's internals never need to know about caching
// or cost.
//
// FLOW (see ask() below, step by step):
// 1. Check the cache first (QueryCache.get()). On a hit, record a
//    zero-latency/zero-cost metric (explicitly showing the SAVINGS from
//    caching) and return immediately -- ragAssistant.ask() is never called.
// 2. On a miss, call the real ragAssistant.ask(), time it, estimate its
//    cost from its token count (via CostEstimator), and record a full
//    QueryMetric.
// 3. Store the fresh response in the cache for next time.
//
// WHY a decorator instead of modifying RagAssistant directly: RagAssistant
// is already independently correct and tested on its own. Wrapping it
// means that logic stays untouched and still independently verifiable --
// this class only ever adds behavior around it, never changes what
// RagAssistant itself does.
//
// SPRING BOOT CONCEPT TO LEARN: this is a plain Java decorator, not a
// Spring-specific mechanism (no @Aspect/AOP here) -- but it solves the
// same "cross-cutting concern" problem AOP is designed for, just done
// explicitly and visibly instead of via proxies/annotations.
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
