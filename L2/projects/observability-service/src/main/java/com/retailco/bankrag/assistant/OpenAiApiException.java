package com.retailco.bankrag.assistant;

// CONCEPT: Custom unchecked exception type.
// PURPOSE: Wraps every possible failure mode of a real OpenAI API call
// (network failure, non-2xx HTTP status, unexpected JSON shape) into one
// exception type callers can catch and handle uniformly, without needing
// to know whether the underlying cause was an IOException, a bad HTTP
// status code, or a parsing bug.
// WHY unchecked (extends RuntimeException, not Exception): OpenAiEmbeddingModel
// and OpenAiLlmClient implement interfaces (EmbeddingModel, LlmClient) whose
// methods don't declare checked exceptions -- a checked exception here
// would force every interface and every caller up the stack to declare or
// catch it, purely because of one implementation's networking detail.
public class OpenAiApiException extends RuntimeException {

    public OpenAiApiException(String message) {
        super(message);
    }

    public OpenAiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
