package com.retailco.bankrag.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * End-to-end demo of the full RAG pipeline described in L2's reference
 * guide, Part I, sections 2-6: Data Collection -> Chunking -> Embedding ->
 * Vector Storage -> Retrieval -> (Generation is out of scope for L2/UC1,
 * which is retrieval-only per the brief: "Establish a strong foundation...
 * capable of retrieving and answering policy-related banking queries").
 *
 * This class is actually compiled and run in this environment (see
 * reports/retrieval-comparison-summary.md for the captured output) --
 * unlike the Spring Boot services in earlier L1 use cases, this module has
 * zero external dependencies, so there is no Maven Central blocker here.
 */
public class Main {

    private static final List<String> SAMPLE_QUERIES = List.of(
            "What is the minimum monthly income required for a personal loan?",
            "What is the interest rate range for a home loan?",
            "What happens if I withdraw my fixed deposit before maturity?",
            "How do I file a complaint and what is the escalation process?",
            "Should I invest my savings in mutual funds right now?" // out-of-scope: no such content in corpus
    );

    public static void main(String[] args) throws IOException {
        Path corpusDir = args.length > 0 ? Path.of(args[0]) : Path.of("corpus");

        DocumentLoader loader = new DocumentLoader();
        List<DocumentLoader.SourceDocument> documents = loader.loadTextDirectory(corpusDir);
        System.out.println("Loaded " + documents.size() + " source documents from " + corpusDir);

        ChunkingConfig config = ChunkingConfig.defaultConfig();
        Chunker chunker = new Chunker(config);
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.isConfigured()
                ? new OpenAiEmbeddingModel()
                : new LocalHashingEmbeddingModel(256);
        System.out.println(OpenAiEmbeddingModel.isConfigured()
                ? "OPENAI_API_KEY detected -- using real OpenAI embeddings."
                : "OPENAI_API_KEY not set -- using offline LocalHashingEmbeddingModel stand-in.");
        VectorStore vectorStore = new VectorStore(embeddingModel);
        KeywordSearcher keywordSearcher = new KeywordSearcher();
        HybridSearcher hybridSearcher = new HybridSearcher(vectorStore, keywordSearcher, 0.6, 0.4);

        int totalChunks = 0;
        for (DocumentLoader.SourceDocument doc : documents) {
            List<Chunk> chunks = chunker.chunk(doc.id(), doc.text());
            for (Chunk chunk : chunks) {
                vectorStore.index(chunk);
            }
            totalChunks += chunks.size();
            System.out.printf("  %-35s -> %d tokens -> %d chunks%n",
                    doc.id(), Chunker.tokenize(doc.text()).length, chunks.size());
        }
        System.out.println("Total chunks indexed: " + totalChunks);
        System.out.println("=".repeat(100));

        double similarityThreshold = 0.15; // below this, we treat retrieval as "no confident match"

        for (String query : SAMPLE_QUERIES) {
            System.out.println("QUERY: " + query);

            List<ScoredChunk> keywordResults = keywordSearcher.search(vectorStore.allChunks(), query, 3);
            List<ScoredChunk> semanticResults = vectorStore.semanticSearch(query, 3, similarityThreshold);
            List<ScoredChunk> hybridResults = hybridSearcher.search(query, 3, similarityThreshold);

            printResults("  KEYWORD ", keywordResults);
            printResults("  SEMANTIC", semanticResults);
            printResults("  HYBRID  ", hybridResults);

            if (semanticResults.isEmpty()) {
                System.out.println("  >>> GUARDRAIL: no chunk cleared the similarity threshold ("
                        + similarityThreshold + "). Correct system behavior: respond \"I don't know\" "
                        + "rather than letting the LLM answer ungrounded. See reports/hallucination-risk-analysis.md.");
            }
            System.out.println("-".repeat(100));
        }
    }

    private static void printResults(String label, List<ScoredChunk> results) {
        if (results.isEmpty()) {
            System.out.println(label + ": (no results above threshold)");
            return;
        }
        for (ScoredChunk sc : results) {
            String preview = sc.chunk().text().substring(0, Math.min(90, sc.chunk().text().length())).replace("\n", " ");
            System.out.printf("%s: score=%.3f  [%s]  \"%s...\"%n",
                    label, sc.score(), sc.chunk().id(), preview);
        }
    }
}
