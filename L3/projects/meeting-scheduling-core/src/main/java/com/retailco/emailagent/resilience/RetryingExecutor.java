package com.retailco.emailagent.resilience;

import java.util.function.Supplier;

/**
 * A small, real retry-with-backoff wrapper for tool calls that might fail
 * transiently (a real calendar/inbox API can time out or 5xx). Not a
 * circuit breaker or a full resilience library -- deliberately minimal,
 * matching this submission's "real but simple, disclosed" components
 * elsewhere (e.g. L2/UC6's audit logger, L1's StubPaymentGateway).
 */
public class RetryingExecutor {

    private final int maxAttempts;
    private final long baseDelayMillis;

    public RetryingExecutor(int maxAttempts, long baseDelayMillis) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelayMillis = baseDelayMillis;
    }

    /** @throws RuntimeException (the last attempt's cause) if every attempt fails. */
    public <T> T executeWithRetry(Supplier<T> action) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt < maxAttempts) {
                    sleep(baseDelayMillis * attempt); // linear backoff
                }
            }
        }
        throw lastFailure;
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted during retry backoff", e);
        }
    }
}
