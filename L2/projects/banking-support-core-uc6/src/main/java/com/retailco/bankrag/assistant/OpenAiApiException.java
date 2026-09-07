package com.retailco.bankrag.assistant;

/** Thrown when a call to the real OpenAI API fails (network, HTTP, or parse error). */
public class OpenAiApiException extends RuntimeException {

    public OpenAiApiException(String message) {
        super(message);
    }

    public OpenAiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
