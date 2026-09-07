package com.retailco.bankrag.observability.controller;

import com.retailco.bankrag.observability.MetricsRecorder;
import com.retailco.bankrag.observability.ObservableRagAssistant;
import com.retailco.bankrag.observability.QueryCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CONCEPT: A read-only "reporting" endpoint -- exposes internal metrics
// state as a GET response instead of only writing it to logs/files.
// PURPOSE: Lets a caller (dashboard, monitoring tool, curl) see live cache
// hit rates and aggregate query metrics (tokens, latency percentiles,
// cost) without restarting the app or reading a log file. A real
// production deployment would typically export these same numbers to
// Prometheus/Grafana instead of (or in addition to) a custom endpoint like
// this; the underlying data is identical either way.
// NOTE: `MetricsReport` is a small record declared INSIDE the controller,
// since it exists purely to bundle this one endpoint's two pieces of data
// (cache + query stats) and has no other use elsewhere.
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
