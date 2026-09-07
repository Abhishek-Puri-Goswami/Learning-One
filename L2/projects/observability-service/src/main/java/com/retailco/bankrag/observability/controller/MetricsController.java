package com.retailco.bankrag.observability.controller;

import com.retailco.bankrag.observability.MetricsRecorder;
import com.retailco.bankrag.observability.ObservableRagAssistant;
import com.retailco.bankrag.observability.QueryCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A read-only endpoint that exposes our internal metrics as a simple GET
 * response, instead of only writing them to a log file. This lets anyone
 * (a dashboard, a monitoring tool, or just curl) see live cache hit rates
 * and aggregate stats (tokens, latency, cost) without restarting the app
 * or digging through a log. A real production system would typically also
 * export these same numbers to a dedicated monitoring tool like
 * Prometheus or Grafana.
 * <p>
 * {@code MetricsReport} is declared right inside this controller, since
 * it exists only to bundle this one endpoint's two pieces of data
 * together and has no other use anywhere else.
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
