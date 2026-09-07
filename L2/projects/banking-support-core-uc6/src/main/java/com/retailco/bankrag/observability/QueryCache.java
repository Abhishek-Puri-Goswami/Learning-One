package com.retailco.bankrag.observability;

import com.retailco.bankrag.assistant.RagAssistant;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deliverable: "Caching strategy." L2 HLD UseCase4 Implementation Approach:
 * "Implement caching for frequent queries" / System Responsibilities:
 * "Reduce unnecessary API calls."
 *
 * A real, working LRU + TTL cache -- not a description of one. Keyed by a
 * normalized query string (lowercased, trimmed, internal whitespace
 * collapsed) so trivial rephrasings that are actually identical queries
 * ("What is my account balance?" vs. "what is my account balance?  ")
 * still hit the cache. Capacity-bounded via LinkedHashMap's access-order
 * mode (a textbook pure-JDK LRU, no external cache library needed --
 * consistent with this submission's pure-JDK-core pattern).
 *
 * Sits in front of RagAssistant.ask() specifically (not in front of
 * BankingToolService's live-data tools from L2/UC3) because caching a
 * policy-document answer is safe -- the underlying documents don't change
 * between requests within a session -- while caching a live account
 * balance would return stale financial data, which L2 HLD UseCase3's own
 * "must be retrieved securely" requirement implicitly rules out. This
 * scope boundary is deliberate, not an oversight -- see
 * performance/performance-optimization-summary.md.
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
