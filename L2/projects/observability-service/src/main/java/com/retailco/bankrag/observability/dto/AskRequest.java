package com.retailco.bankrag.observability.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(@NotBlank(message = "query is required") String query) {
}
