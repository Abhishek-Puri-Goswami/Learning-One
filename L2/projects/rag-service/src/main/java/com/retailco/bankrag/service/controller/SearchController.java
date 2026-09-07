package com.retailco.bankrag.service.controller;

import com.retailco.bankrag.service.dto.SearchResponse;
import com.retailco.bankrag.service.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// CONCEPT: Controller layer -- exposes retrieval as a GET endpoint with
// query parameters.
// PURPOSE: GET /api/v1/rag/search?query=...&method=HYBRID&topK=3 lets a
// caller choose which retrieval strategy to use (KEYWORD/SEMANTIC/HYBRID)
// and get back ranked results with relevance scores.
// HOW Spring binds the parameters: `@RequestParam` maps URL query
// parameters directly to method parameters, including automatic
// String -> enum conversion for `method` (Spring calls
// SearchService.Method.valueOf(...) under the hood) and a `defaultValue`
// used when the parameter is omitted from the request.
// WHY this stays a thin wrapper: same Controller-Service pattern as
// IngestionController -- all the actual search logic (which searcher to
// use, guardrail checks) lives in SearchService, not here.
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
