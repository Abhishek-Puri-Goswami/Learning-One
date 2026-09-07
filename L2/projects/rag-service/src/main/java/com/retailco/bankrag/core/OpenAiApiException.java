package com.retailco.bankrag.core;

/**
 * Wraps up every possible way a call to OpenAI's API can fail (a network
 * problem, an error status code, or unexpected data) into ONE exception
 * type. That way, whoever calls into this code only has to handle one
 * kind of error, instead of needing to know exactly which underlying
 * problem occurred.
 */
public class OpenAiApiException extends RuntimeException {

    public OpenAiApiException(String message) {
        super(message);
    }

    public OpenAiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
