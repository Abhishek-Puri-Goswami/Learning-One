package com.retailco.emailagent.resilience;

import java.util.function.Supplier;

/**
 * Automatically retries a call that might fail temporarily — a real
 * external API can occasionally time out or have a brief hiccup, and we
 * don't want one bad moment to fail the whole operation.
 * <p>
 * Here's how it works: it calls {@code action}, and if that throws an
 * error, it waits a little longer each time (this is called "linear
 * backoff" — the wait time grows with each attempt) before trying again,
 * up to a maximum number of attempts. If every single attempt fails, the
 * last failure is thrown for real, so the caller still finds out
 * something went wrong.
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
