package com.retailco.bankrag.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Defines exactly what shape of JSON {@code POST /api/v1/rag/ingest}
 * accepts. Spring automatically turns the incoming JSON body into this
 * record, and the {@code @NotBlank} annotation below gets checked BEFORE
 * the controller method body even runs — an empty or missing
 * {@code corpusDirectory} never reaches our business logic at all; it's
 * rejected right away with a clean 400 error.
 * <p>
 * Using a dedicated request type like this (instead of passing raw
 * values, or exposing our internal classes directly) keeps our public API
 * shape stable even if internal classes ever change — only the fields
 * meant to be part of the public contract are exposed here.
 */
public record IngestRequest(
        @NotBlank(message = "corpusDirectory is required")
        String corpusDirectory,
        Integer chunkSizeTokens,
        Integer overlapTokens
) {
}
