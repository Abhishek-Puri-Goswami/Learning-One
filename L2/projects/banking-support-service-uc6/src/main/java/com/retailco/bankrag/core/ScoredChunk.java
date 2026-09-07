package com.retailco.bankrag.core;

/**
 * Pairs one {@link Chunk} with a number showing how relevant it is to a
 * search. Every search method in this package returns results in this
 * same shape, so whatever code displays or sorts the results doesn't need
 * to care which search method actually produced them.
 */
public record ScoredChunk(Chunk chunk, double score) {
}
