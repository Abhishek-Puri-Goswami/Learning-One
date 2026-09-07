package com.retailco.bankrag.service.dto;

import jakarta.validation.constraints.NotBlank;

// CONCEPT: DTO (Data Transfer Object), implemented as a `record`, with
// Jakarta Bean Validation annotations.
// PURPOSE: Defines exactly what JSON shape POST /api/v1/rag/ingest
// accepts. Spring automatically deserializes the incoming JSON body into
// this record (via Jackson), and `@Valid` on the controller parameter
// triggers validation of the `@NotBlank` constraint BEFORE the controller
// method body runs -- an empty/missing corpusDirectory never reaches
// IngestionService at all; it's rejected with a 400 by
// GlobalExceptionHandler's MethodArgumentNotValidException handler.
// WHY a DTO instead of passing raw parameters or exposing domain classes
// directly: it decouples the REST API's shape from internal classes like
// ChunkingConfig -- the API can stay stable even if internal types change,
// and only the fields meant to be part of the public contract are exposed.
public record IngestRequest(
        @NotBlank(message = "corpusDirectory is required")
        String corpusDirectory,
        Integer chunkSizeTokens,
        Integer overlapTokens
) {
}
