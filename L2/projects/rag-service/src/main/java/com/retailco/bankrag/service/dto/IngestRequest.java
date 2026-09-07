package com.retailco.bankrag.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/v1/rag/ingest.
 *
 * corpusDirectory points at a directory of .txt policy documents on disk
 * (mirrors rag-core/corpus/ used in the actually-executed demo run --
 * see reports/retrieval-demo-run-log.txt). A production ingestion
 * controller would instead accept multipart file uploads or an object
 * storage key; a directory path is the deliberately simple foundation-scope
 * choice here, consistent with DocumentLoader.loadTextDirectory's signature.
 */
public record IngestRequest(
        @NotBlank(message = "corpusDirectory is required")
        String corpusDirectory,
        Integer chunkSizeTokens,
        Integer overlapTokens
) {
}
