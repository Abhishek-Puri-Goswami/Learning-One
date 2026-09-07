package com.retailco.bankrag.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The very first step of our RAG pipeline: reading raw text files off
 * disk. Each file becomes a {@code SourceDocument} — a simple pairing of
 * its filename and its full text — which is exactly what the rest of the
 * pipeline ({@code Chunker}, then {@code VectorStore}) expects as input.
 * <p>
 * Only plain {@code .txt} files are supported right now, since our sample
 * documents are already plain text. A real system could add support for
 * PDF or Word documents right here, and nothing downstream would need to
 * change at all — every other class only ever depends on the simple
 * "filename + text" shape this class produces.
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
