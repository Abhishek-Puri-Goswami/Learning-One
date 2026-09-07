package com.retailco.bankrag.service.controller;

import com.retailco.bankrag.service.dto.SearchResponse;
import com.retailco.bankrag.service.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Step 2 of the "Foundation & Core Retrieval" REST surface: retrieval.
 * Exposes the same keyword/semantic/hybrid comparison exercised in
 * reports/retrieval-comparison-summary.md as a callable API, plus the
 * hallucination guardrail from reports/hallucination-risk-analysis.md.
 *
 * Example: GET /api/v1/rag/search?query=What+is+the+interest+rate+range+for+a+home+loan&method=HYBRID&topK=3
 */
@RestController
@RequestMapping("/api/v1/rag")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "HYBRID") SearchService.Method method,
            @RequestParam(defaultValue = "3") int topK) {
        return ResponseEntity.ok(searchService.search(query, method, topK));
    }
}
