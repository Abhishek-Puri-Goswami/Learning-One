package com.retailco.bankrag.observability;

import com.retailco.bankrag.assistant.RagAssistant;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A cache that remembers recent question-and-answer pairs, so we don't
 * have to re-run the whole retrieval + AI pipeline for a question that
 * was already answered recently — saving both time and real API cost.
 * This is an LRU ("Least Recently Used") cache with a time limit on how
 * long entries stay valid, built entirely from plain Java collections,
 * with no external caching library.
 * <p>
 * How the "least recently used" eviction works (see the constructor
 * below): a {@code LinkedHashMap} with {@code accessOrder=true} quietly
 * reorders itself so the most-recently-used entry always moves to the
 * end every time it's read. Overriding {@code removeEldestEntry()} makes
 * the map automatically remove the entry at the FRONT — the least
 * recently used one — whenever it grows past the allowed size. This is
 * the standard, library-free way to build an LRU cache in plain Java.
 * <p>
 * How the time limit works: each cached entry records when it was
 * created. {@code get()} checks whether that entry is too old before
 * returning it — an expired entry is treated as if it were never cached,
 * even if the LRU logic hasn't gotten around to removing it yet.
 * <p>
 * Why we normalize the cache key: "What is my balance?" and
 * "what is my balance?  " mean the same thing to a person, so treating
 * them as different cache keys would waste cache space on trivial
 * formatting differences.
 * <p>
 * An important, deliberate boundary: this cache is used ONLY for policy
 * Q&A, never for live banking data like account balances. Policy
 * documents don't change between requests, so caching them is safe — but
 * a cached account balance could show a customer stale, wrong financial
 * data, which is never acceptable.
 */
public class QueryCache {

    public record CacheEntry(RagAssistant.AssistantResponse response, Instant cachedAt) {
        boolean isExpired(Instant now, long ttlSeconds) {
            return now.getEpochSecond() - cachedAt.getEpochSecond() > ttlSeconds;
        }
    }

    private final int maxEntries;
    private final long ttlSeconds;
    private final LinkedHashMap<String, CacheEntry> store;

    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;
    private long expirations = 0;

    public QueryCache(int maxEntries, long ttlSeconds) {
        this.maxEntries = maxEntries;
        this.ttlSeconds = ttlSeconds;
        // accessOrder=true is what turns this into a proper LRU cache:
        // every get() moves that entry to the end of the map, so the entry
        // removed by removeEldestEntry() is always the one that hasn't
        // been touched in the longest time — not just the oldest one added.
        this.store = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                boolean shouldEvict = size() > QueryCache.this.maxEntries;
                if (shouldEvict) {
                    evictions++;
                }
                return shouldEvict;
            }
        };
    }

    public RagAssistant.AssistantResponse get(String rawQuery) {
        String key = normalize(rawQuery);
        CacheEntry entry = store.get(key);
        if (entry == null) {
            misses++;
            return null;
        }
        if (entry.isExpired(Instant.now(), ttlSeconds)) {
            store.remove(key);
            expirations++;
            misses++;
            return null;
        }
        hits++;
        return entry.response();
    }

    public void put(String rawQuery, RagAssistant.AssistantResponse response) {
        // Blocked and fallback responses get cached too, not just
        // successful answers — a repeated prompt-injection attempt
        // shouldn't re-run the whole pipeline any more than a repeated
        // real question should. Caching only the "good" answers would
        // miss most of the real savings this cache is meant to provide.
        store.put(normalize(rawQuery), new CacheEntry(response, Instant.now()));
    }

    public record CacheStats(long hits, long misses, long evictions, long expirations, int currentSize, double hitRate) {
    }

    public CacheStats stats() {
        long total = hits + misses;
        double hitRate = total == 0 ? 0.0 : (double) hits / total;
        return new CacheStats(hits, misses, evictions, expirations, store.size(), hitRate);
    }

    private String normalize(String query) {
        return query.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
