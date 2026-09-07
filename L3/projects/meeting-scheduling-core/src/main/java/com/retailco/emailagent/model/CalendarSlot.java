package com.retailco.emailagent.model;

import java.time.Instant;

// CONCEPT: Value object (record) with self-validation -- a candidate
// meeting time window plus a relevance score.
/**
 * A candidate meeting slot returned by the calendar tool. Field names match
 * the LLD's `/calendar/availability` response shape (`start`, `end`,
 * `score`) so a real Graph/Google Calendar adapter would produce the same
 * wire shape -- only the transport (a mock, in-memory free/busy store here)
 * differs.
 */
public record CalendarSlot(Instant start, Instant end, double score) {

    public CalendarSlot {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start: " + start + " -> " + end);
        }
    }
}
