package com.retailco.emailagent.model;

import java.time.Instant;

/**
 * One candidate meeting time returned by the calendar tool — a start
 * time, an end time, and a score showing how well it matches what the
 * sender asked for. The constructor below checks that {@code end} always
 * comes after {@code start}, so this object can never represent a
 * nonsensical time range.
 */
public record CalendarSlot(Instant start, Instant end, double score) {

    public CalendarSlot {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start: " + start + " -> " + end);
        }
    }
}
