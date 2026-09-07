package com.retailco.bankrag.core;

import java.util.List;

/**
 * Hand-rolled test harness -- NOT JUnit. Maven Central is unreachable from
 * this sandbox (same limitation noted throughout L1), so a JUnit jar could
 * not be fetched either. This runs as a plain `java` program and prints
 * PASS/FAIL per assertion, exiting non-zero if anything fails, so it can
 * still gate a CI step. Replace with real JUnit 5 tests once this module is
 * built on a machine with normal Maven Central access (see README.md).
 */
public class SelfTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testChunkerProducesOverlappingWindows();
        testChunkerHandlesShortTextAsSingleChunk();
        testEmbeddingModelIsDeterministic();
        testEmbeddingModelIsNormalized();
        testVectorStoreRanksMostSimilarFirst();
        testVectorStoreRespectsSimilarityThreshold();
        testKeywordSearcherMatchesExactTerms();
        testKeywordSearcherHandlesRepeatedWordsWithoutCrashing();
        testHybridSearcherCombinesBothSignals();

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("RESULTS: " + passed + " passed, " + failed + " failed");
        System.out.println("=".repeat(60));
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void testChunkerProducesOverlappingWindows() {
        String text = String.join(" ", java.util.Collections.nCopies(500, "word"));
        Chunker chunker = new Chunker(new ChunkingConfig(180, 40));
        List<Chunk> chunks = chunker.chunk("doc1", text);

        // 500 tokens, window 180, step 140 -> ceil((500-180)/140)+1 = 4 windows
        check("chunker produces expected chunk count", chunks.size() == 4, "expected 4, got " + chunks.size());
        check("first chunk has exactly chunkSizeTokens tokens",
                Chunker.tokenize(chunks.get(0).text()).length == 180,
                "got " + Chunker.tokenize(chunks.get(0).text()).length);
        check("chunks carry the correct source document id",
                chunks.get(0).sourceDocument().equals("doc1"), chunks.get(0).sourceDocument());
    }

    static void testChunkerHandlesShortTextAsSingleChunk() {
        Chunker chunker = new Chunker(ChunkingConfig.defaultConfig());
        List<Chunk> chunks = chunker.chunk("short-doc", "This is a very short policy sentence.");
        check("short text yields exactly one chunk", chunks.size() == 1, "got " + chunks.size());
    }

    static void testEmbeddingModelIsDeterministic() {
        EmbeddingModel model = new LocalHashingEmbeddingModel(64);
        double[] v1 = model.embed("home loan interest rate");
        double[] v2 = model.embed("home loan interest rate");
        check("same text embeds to the same vector", java.util.Arrays.equals(v1, v2), "vectors differed");
    }

    static void testEmbeddingModelIsNormalized() {
        EmbeddingModel model = new LocalHashingEmbeddingModel(64);
        double[] v = model.embed("fixed deposit premature withdrawal penalty");
        double normSq = 0;
        for (double x : v) normSq += x * x;
        check("embedding is L2-normalized (norm ~= 1.0)",
                Math.abs(Math.sqrt(normSq) - 1.0) < 1e-9, "norm was " + Math.sqrt(normSq));
    }

    static void testVectorStoreRanksMostSimilarFirst() {
        EmbeddingModel model = new LocalHashingEmbeddingModel(128);
        VectorStore store = new VectorStore(model);
        store.index(new Chunk("c1", "loans", 0, "home loan interest rate is eight point two five percent"));
        store.index(new Chunk("c2", "fd", 0, "fixed deposit premature withdrawal penalty applies"));
        store.index(new Chunk("c3", "grievance", 0, "customer complaint escalation matrix has three levels"));

        List<ScoredChunk> results = store.semanticSearch("what is the home loan interest rate", 3, 0.0);
        check("most relevant chunk (loans) ranks first",
                !results.isEmpty() && results.get(0).chunk().id().equals("c1"),
                "top result was " + (results.isEmpty() ? "none" : results.get(0).chunk().id()));
    }

    static void testVectorStoreRespectsSimilarityThreshold() {
        EmbeddingModel model = new LocalHashingEmbeddingModel(128);
        VectorStore store = new VectorStore(model);
        store.index(new Chunk("c1", "loans", 0, "home loan interest rate is eight point two five percent"));

        List<ScoredChunk> results = store.semanticSearch(
                "completely unrelated query about zoo animals and giraffes", 3, 0.9);
        check("unrelated query with a high threshold returns no results",
                results.isEmpty(), "expected empty, got " + results.size() + " results");
    }

    static void testKeywordSearcherMatchesExactTerms() {
        KeywordSearcher searcher = new KeywordSearcher();
        List<Chunk> chunks = List.of(
                new Chunk("c1", "loans", 0, "home loan interest rate eight point two five percent"),
                new Chunk("c2", "fd", 0, "fixed deposit premature withdrawal penalty")
        );
        List<ScoredChunk> results = searcher.search(chunks, "home loan interest rate", 5);
        check("keyword search finds the chunk containing the exact terms",
                !results.isEmpty() && results.get(0).chunk().id().equals("c1"),
                "top result was " + (results.isEmpty() ? "none" : results.get(0).chunk().id()));
    }

    static void testKeywordSearcherHandlesRepeatedWordsWithoutCrashing() {
        KeywordSearcher searcher = new KeywordSearcher();
        List<Chunk> chunks = List.of(new Chunk("c1", "doc", 0, "the the the loan loan policy policy applies applies"));
        try {
            List<ScoredChunk> results = searcher.search(chunks, "the loan policy", 5);
            check("repeated-word text does not throw (HashSet, not Set.of)", true, "");
            check("repeated-word chunk still matches", !results.isEmpty(), "expected a match");
        } catch (IllegalArgumentException e) {
            check("repeated-word text does not throw (HashSet, not Set.of)", false, e.getMessage());
        }
    }

    static void testHybridSearcherCombinesBothSignals() {
        EmbeddingModel model = new LocalHashingEmbeddingModel(128);
        VectorStore store = new VectorStore(model);
        KeywordSearcher keywordSearcher = new KeywordSearcher();
        store.index(new Chunk("c1", "loans", 0, "home loan interest rate is eight point two five percent"));
        store.index(new Chunk("c2", "fd", 0, "fixed deposit premature withdrawal penalty applies here"));

        HybridSearcher hybrid = new HybridSearcher(store, keywordSearcher, 0.6, 0.4);
        List<ScoredChunk> results = hybrid.search("home loan interest rate", 2, 0.0);

        check("hybrid search returns results", !results.isEmpty(), "expected non-empty results");
        check("hybrid search ranks the term-matching chunk first",
                results.get(0).chunk().id().equals("c1"), "top result was " + results.get(0).chunk().id());
    }

    private static void check(String description, boolean condition, String failureDetail) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            passed++;
        } else {
            System.out.println("  [FAIL] " + description + " -- " + failureDetail);
            failed++;
        }
    }
}
