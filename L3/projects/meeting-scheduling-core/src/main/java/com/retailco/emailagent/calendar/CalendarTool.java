package com.retailco.emailagent.calendar;

import com.retailco.emailagent.model.CalendarSlot;
import com.retailco.emailagent.nlp.TimeHint;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is what's called an "AI tool" — a plain, ordinary method the agent
 * calls to get real, factual information (here, calendar availability)
 * instead of letting an AI model just guess or make something up.
 * <p>
 * Right now this class simulates a calendar with a simple in-memory
 * free/busy list per participant. A real version of this class would call
 * Microsoft Graph's or Google Calendar's actual availability API instead
 * — but it would return the exact same shape of answer, so nothing else
 * in the codebase would need to change.
 * <p>
 * Here's how the search works: it looks at business hours (9am-5pm UTC)
 * over the next several days, and scores each open slot by how well it
 * matches what the sender asked for (same day of week, same part of day,
 * how close to any specific time they mentioned — see
 * {@code scoreSlot()}). That scoring is what lets us offer the "2 best"
 * matching times instead of just the first 2 that happen to be free. If
 * fewer than 2 slots turn up, the search automatically looks further
 * ahead instead of giving up too early.
 */
public class CalendarTool {

    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(17, 0);

    private final Map<String, Set<Instant>> busyStartTimesByParticipant = new ConcurrentHashMap<>();
    private final Clock clock;

    public CalendarTool(Clock clock) {
        this.clock = clock;
    }

    /** Marks [start, start+duration) as busy for a participant -- used by fixtures/tests to seed conflicts. */
    public void markBusy(String participant, Instant start, Duration duration) {
        Set<Instant> busy = busyStartTimesByParticipant.computeIfAbsent(participant,
                k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
        // Mark every 30-minute grid slot the busy period overlaps.
        Instant cursor = start;
        while (cursor.isBefore(start.plus(duration))) {
            busy.add(cursor);
            cursor = cursor.plus(Duration.ofMinutes(30));
        }
    }

    public List<CalendarSlot> findAvailableSlots(List<String> participants, Duration duration, TimeHint hint) {
        return findAvailableSlots(participants, duration, hint, 5);
    }

    /** @param searchWindowDays widened automatically by the caller pattern below if fewer than 2 slots are found. */
    public List<CalendarSlot> findAvailableSlots(List<String> participants, Duration duration, TimeHint hint,
                                                  int searchWindowDays) {
        List<CalendarSlot> candidates = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);

        for (int dayOffset = 1; dayOffset <= searchWindowDays; dayOffset++) {
            LocalDate day = nextBusinessDay(today, dayOffset);
            for (LocalTime t = BUSINESS_START; !t.plus(duration).isAfter(BUSINESS_END); t = t.plusMinutes(30)) {
                Instant slotStart = LocalDateTime.of(day, t).toInstant(ZoneOffset.UTC);
                Instant slotEnd = slotStart.plus(duration);
                if (isFreeForAll(participants, slotStart, duration)) {
                    double score = scoreSlot(day, t, hint);
                    candidates.add(new CalendarSlot(slotStart, slotEnd, score));
                }
            }
        }

        candidates.sort((a, b) -> Double.compare(b.score(), a.score())); // highest score first

        if (candidates.size() < 2 && searchWindowDays < 20) {
            // Widen the search rather than returning fewer than the acceptance
            // criterion's minimum of 2 slots, as long as any capacity exists at all.
            return findAvailableSlots(participants, duration, hint, searchWindowDays * 2);
        }
        return candidates;
    }

    private boolean isFreeForAll(List<String> participants, Instant slotStart, Duration duration) {
        for (String participant : participants) {
            Set<Instant> busy = busyStartTimesByParticipant.getOrDefault(participant, Set.of());
            Instant cursor = slotStart;
            while (cursor.isBefore(slotStart.plus(duration))) {
                if (busy.contains(cursor)) {
                    return false;
                }
                cursor = cursor.plus(Duration.ofMinutes(30));
            }
        }
        return true;
    }

    private double scoreSlot(LocalDate day, LocalTime time, TimeHint hint) {
        double score = 1.0; // baseline: any free slot beats no slot
        if (hint.dayOfWeek().isPresent() && hint.dayOfWeek().get() == day.getDayOfWeek()) {
            score += 3.0;
        }
        if (hint.partOfDay().isPresent()) {
            boolean matches = switch (hint.partOfDay().get()) {
                case MORNING -> time.isBefore(LocalTime.NOON);
                case AFTERNOON -> !time.isBefore(LocalTime.NOON) && time.isBefore(LocalTime.of(17, 0));
                case EVENING -> !time.isBefore(LocalTime.of(17, 0));
            };
            if (matches) score += 2.0;
        }
        if (hint.explicitTime().isPresent()) {
            long minutesApart = Math.abs(Duration.between(hint.explicitTime().get(), time).toMinutes());
            score += Math.max(0, 4.0 - (minutesApart / 30.0));
        }
        return score;
    }

    private LocalDate nextBusinessDay(LocalDate from, int businessDaysAhead) {
        LocalDate d = from;
        int counted = 0;
        while (counted < businessDaysAhead) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                counted++;
            }
        }
        return d;
    }
}
