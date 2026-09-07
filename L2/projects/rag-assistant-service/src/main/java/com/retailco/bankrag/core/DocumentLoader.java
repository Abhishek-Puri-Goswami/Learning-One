package com.retailco.bankrag.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// CONCEPT: Data ingestion / document loading -- step 1 of a RAG pipeline.
// PURPOSE: Reads raw text files off disk and wraps each one in a
// SourceDocument (a small nested record: filename + full text), which is
// the input the rest of the pipeline (Chunker, VectorStore) expects.
// FLOW:
// Directory of .txt files -> DocumentLoader -> List<SourceDocument> ->
// Chunker.chunk(...) -> VectorStore.index(...)
// WHY: Only plain .txt is supported here (the corpus is already extracted
// text). A production system would add PDF/DOCX/HTML parsers at this same
// point without touching anything downstream, because everything after
// this class only depends on the simple SourceDocument shape.
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
