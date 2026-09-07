package com.retailco.bankrag.observability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records one entry per question asked (was it a cache hit? blocked? how
 * long did it take? how many tokens did it use? what did it cost?), kept
 * simply in memory rather than using an external metrics library.
 * {@code aggregate()} then turns that raw list into summary statistics:
 * totals, averages, and latency percentiles.
 * <p>
 * Why bother with p50/p95 latency instead of just an average: an average
 * can hide a bad "tail" — imagine most requests are fast, but 5% are very
 * slow. The average alone would look fine, hiding a real problem. p50
 * (the median) tells you the TYPICAL experience; p95 tells you what the
 * slower end looks like — which is usually what actually gets noticed and
 * complained about.
 * <p>
 * {@code percentile()} uses a simple, exact method that works great for
 * the small number of samples this demo produces. A production system
 * handling thousands of requests per second would typically use a more
 * advanced, memory-efficient algorithm instead of storing every single
 * raw measurement — but the idea is the same.
 */
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
     * Picks the value at a given percentile position out of an
     * already-sorted list — for example, p=0.95 picks the value that 95%
     * of the samples fall at or below.
     */
    private long percentile(List<Long> sortedValues, double p) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(p * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
}
