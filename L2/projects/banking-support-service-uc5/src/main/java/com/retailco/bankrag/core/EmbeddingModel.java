package com.retailco.bankrag.core;

/**
 * This interface defines the ONE thing every "embedding model" must be
 * able to do: turn a piece of text into a list of numbers (a "vector")
 * that captures its meaning, and report how many numbers are in that
 * vector. Nothing else in the codebase needs to know HOW that vector gets
 * produced — only that it can be.
 * <p>
 * Why use an interface here at all? It lets the rest of the app (like
 * {@code VectorStore}, or the Spring configuration classes) depend only
 * on "some EmbeddingModel," without caring whether it's really talking to
 * OpenAI's real embedding API or a simple offline stand-in. Deciding
 * which one to actually use happens in exactly ONE place — the
 * {@code *Config.java} file in each module — so swapping between them
 * never requires touching this class or anything that uses it. This is a
 * classic design idea called the "Strategy pattern."
 * <p>
 * One important rule: whichever implementation you pick MUST be used
 * consistently for both storing documents AND searching for them. Mixing
 * vectors from two different embedding models when comparing them for
 * similarity produces meaningless results — see {@code VectorStore} for
 * where that consistency is enforced.
 * <p>
 * Spring Boot concept to notice: this is called "programming to an
 * interface" — the {@code @Bean} methods in each {@code *Config.java}
 * class decide which real class gets plugged in wherever an
 * {@code EmbeddingModel} is needed.
 */
public interface EmbeddingModel {
    double[] embed(String text);

    int dimensions();
}
