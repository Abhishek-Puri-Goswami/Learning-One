package com.retailco.bankrag.assistant.dto;

import jakarta.validation.constraints.NotBlank;

public record IngestRequest(
        @NotBlank(message = "corpusDirectory is required")
        String corpusDirectory
) {
}
