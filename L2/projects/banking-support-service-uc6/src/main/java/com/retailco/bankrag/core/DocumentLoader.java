package com.retailco.bankrag.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Step 2 of the RAG pipeline (L2 reference guide, "Document Loading"):
 * loads raw text documents from a directory. Reads plain .txt files here
 * (the corpus/ folder contains text already extracted from
 * L2/_Reference-Docs/Secure_Bank_policy_Manual_input-docs.pdf); a production
 * ingestion pipeline would add PDF/DOCX/HTML loaders per
 * design/embedding-generation-module.md's "swap points" list.
 */
public class DocumentLoader {

    public record SourceDocument(String id, String text) {
    }

    public List<SourceDocument> loadTextDirectory(Path directory) throws IOException {
        List<SourceDocument> documents = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> sorted = files.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
            for (Path file : sorted) {
                String text = Files.readString(file);
                documents.add(new SourceDocument(file.getFileName().toString(), text));
            }
        }
        return documents;
    }
}
