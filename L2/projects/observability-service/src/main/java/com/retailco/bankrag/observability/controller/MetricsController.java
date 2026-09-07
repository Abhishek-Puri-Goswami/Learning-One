package com.retailco.bankrag.observability.controller;

import com.retailco.bankrag.observability.MetricsRecorder;
import com.retailco.bankrag.observability.ObservableRagAssistant;
import com.retailco.bankrag.observability.QueryCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deliverable: "Observability metrics report" as a live, queryable
 * endpoint rather than only a point-in-time offline report. A real
 * deployment would export these same numbers to Prometheus/Grafana; this
 * endpoint is the same data, directly readable, for this submission's
 * scope.
 */
@RestController
@RequestMapping("/api/v1/observability")
public class MetricsController {

    private final ObservableRagAssistant observableRagAssistant;

    public MetricsController(ObservableRagAssistant observableRagAssistant) {
        this.observableRagAssistant = observableRagAssistant;
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsReport> metrics() {
        QueryCache.CacheStats cacheStats = observableRagAssistant.cacheStats();
        MetricsRecorder.AggregateReport aggregate = observableRagAssistant.metricsReport();
        return ResponseEntity.ok(new MetricsReport(cacheStats, aggregate));
    }

    public record MetricsReport(QueryCache.CacheStats cache, MetricsRecorder.AggregateReport queries) {
    }
}
