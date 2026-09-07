package com.retailco.bankrag.observability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// CONCEPT: Observability metrics collection -- token usage and latency
// tracking, computed in-memory (no external metrics library like
// Micrometer/Prometheus client).
// PURPOSE: Records one QueryMetric per RagAssistant.ask() call (was it a
// cache hit? blocked? fallback? how long did it take? how many tokens?
// what did it cost?), then aggregate() turns that raw list into summary
// statistics: totals, averages, and latency percentiles.
//
// WHY p50/p95 latency, not just an average: an average can hide a bad tail
// -- e.g. most queries fast but 5% very slow. p50 (median) is a robust
// "typical" experience; p95 shows what the slower end of real usage looks
// like, which is what actually gets noticed/complained about in a real
// product.
//
// HOW percentile() WORKS (nearest-rank method, see below): given a
// pre-sorted list of latencies, index = ceil(p * count) - 1 picks the
// value at the requested percentile position directly, clamped to valid
// bounds. This is exact and simple for a small sample -- a
// production system with thousands of samples per window would typically
// use a streaming/approximate percentile algorithm (HDRHistogram,
// t-digest) instead of storing every raw sample.
//
// WHY an in-memory list (not Micrometer/Prometheus): keeps this module
// dependency-free; a production deployment would export these SAME
// numbers to a real metrics backend without changing what's measured --
// only where it's reported.
public class MetricsRecorder {

    public record QueryMetric(String query, boolean cacheHit, boolean blocked, boolean fallback,
                               long latencyMs, int totalTokens, double estimatedCostUsd) {
    }

    private final List<QueryMetric> metrics = new ArrayList<>();

    public void record(QueryMetric metric) {
        metrics.add(metric);
    }

    public record AggregateReport(
            int totalQueries,
            int cacheHits,
            int cacheMisses,
            int blockedByGuardrail,
            int fallbackResponses,
            int answeredQueries,
            long totalTokens,
            double avgTokensPerAnsweredQuery,
            double avgLatencyMs,
            long p50LatencyMs,
            long p95LatencyMs,
            double totalEstimatedCostUsd
    ) {
    }

    public AggregateReport aggregate() {
        int total = metrics.size();
        int hits = (int) metrics.stream().filter(MetricsRecorder.QueryMetric::cacheHit).count();
        int blocked = (int) metrics.stream().filter(MetricsRecorder.QueryMetric::blocked).count();
        int fallback = (int) metrics.stream().filter(MetricsRecorder.QueryMetric::fallback).count();
        int answered = total - blocked - fallback;

        long totalTokens = metrics.stream().mapToLong(QueryMetric::totalTokens).sum();
        double avgTokens = answered == 0 ? 0.0 : metrics.stream()
                .filter(m -> !m.blocked() && !m.fallback())
                .mapToInt(QueryMetric::totalTokens).average().orElse(0.0);

        List<Long> latencies = metrics.stream().map(QueryMetric::latencyMs).sorted().toList();
        double avgLatency = latencies.isEmpty() ? 0.0 : latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p50 = percentile(latencies, 0.50);
        long p95 = percentile(latencies, 0.95);

        double totalCost = metrics.stream().mapToDouble(QueryMetric::estimatedCostUsd).sum();

        return new AggregateReport(total, hits, total - hits, blocked, fallback, answered,
                totalTokens, avgTokens, avgLatency, p50, p95, totalCost);
    }

    public List<QueryMetric> allMetrics() {
        return Collections.unmodifiableList(metrics);
    }

    /**
     * Nearest-rank percentile over a pre-sorted list -- simple and exactly
     * correct for the small sample sizes this demo actually produces (a
     * handful of queries); a production system with thousands of samples
     * per window would more likely use a streaming/approximate percentile
     * algorithm (e.g. HDRHistogram or t-digest), noted as a scale-up path
     * rather than implemented here (out of this use case's scope, and not
     * reachable via Maven Central in this sandbox regardless).
     */
    private long percentile(List<Long> sortedValues, double p) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(p * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
}
