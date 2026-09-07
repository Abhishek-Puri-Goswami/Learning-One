package com.retailco.bankrag.observability;

import com.retailco.bankrag.assistant.RagAssistant;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// CONCEPT: Caching -- specifically an LRU (Least Recently Used) cache with
// TTL (Time To Live) expiry, built from plain JDK collections (no external
// cache library like Caffeine/Ehcache).
// PURPOSE: Avoids re-running the full RagAssistant pipeline (retrieval +
// LLM call) for a question that was already answered recently -- saving
// latency and real API cost.
//
// HOW THE LRU WORKS (see the constructor below): a `LinkedHashMap` built
// with `accessOrder=true` reorders itself so the most-recently-accessed
// entry moves to the end every time `get()` is called. Overriding
// `removeEldestEntry()` to return true once `size() > maxEntries` makes
// the map automatically evict the entry at the front (the LEAST recently
// used one) whenever a new entry would exceed capacity -- this is the
// textbook pure-JDK way to build an LRU cache without any library.
//
// HOW TTL WORKS: each CacheEntry records when it was cached (cachedAt).
// `get()` checks `isExpired()` before returning a hit -- an expired entry
// is treated as a miss and removed, even though the LRU eviction alone
// wouldn't have removed it yet.
//
// WHY normalize the cache key (see normalize()): "What is my balance?" and
// "what is my balance?  " are the same question to a human, so treating
// them as different cache keys would waste cache capacity and hit rate on
// trivial formatting differences.
//
// IMPORTANT (scope boundary, a deliberate design decision): this cache
// sits ONLY in front of RagAssistant.ask() (policy Q&A), never in front of
// live banking data (account balance, transactions). Policy documents
// don't change between requests, so caching them is safe; a cached
// account balance could return stale/wrong financial data, which is
// unacceptable for a security-sensitive banking use case.
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
        // accessOrder=true turns this into a proper LRU: get() moves the
        // entry to the end, so removeEldestEntry evicts the least-recently-
        // used entry, not just the oldest-inserted one.
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
        // Guardrail-blocked and fallback responses ARE cached too -- a
        // repeated prompt-injection attempt or a repeated weak-retrieval
        // query shouldn't re-run the full pipeline any more than a
        // successful answer should. Caching only "good" answers would
        // under-count real savings and miss the actual point of this
        // deliverable (reduce unnecessary API/compute calls generally).
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
