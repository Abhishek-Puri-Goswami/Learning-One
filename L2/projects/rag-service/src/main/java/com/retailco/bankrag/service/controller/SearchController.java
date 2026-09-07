package com.retailco.bankrag.service.controller;

import com.retailco.bankrag.service.dto.SearchResponse;
import com.retailco.bankrag.service.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes search as a GET endpoint:
 * {@code GET /api/v1/rag/search?query=...&method=HYBRID&topK=3} lets a
 * caller choose which search strategy to use and get back ranked results
 * with relevance scores.
 * <p>
 * {@code @RequestParam} below is what maps URL query parameters directly
 * onto this method's parameters, including automatically converting the
 * text {@code method} into the matching {@code SearchService.Method}
 * enum value, and falling back to a default value when a parameter is
 * left out of the request.
 * <p>
 * Just like {@code IngestionController}, this stays a thin wrapper — the
 * actual search logic lives entirely in {@code SearchService}.
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
