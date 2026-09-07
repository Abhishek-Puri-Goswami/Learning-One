package com.retailco.bankrag.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * L2 UC1 deliverable: production-shaped Spring Boot entry point wrapping
 * rag-core's chunking/embedding/retrieval pipeline in a REST API.
 *
 * See ../../core/* for the actually-compiled-and-run retrieval logic
 * (copied here from rag-core/ — see this module's pom.xml for why it is
 * a source copy rather than a Maven dependency in this sandbox) and
 * ../../../../reports/ for real captured execution output.
 */
@SpringBootApplication
public class RagServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
}
