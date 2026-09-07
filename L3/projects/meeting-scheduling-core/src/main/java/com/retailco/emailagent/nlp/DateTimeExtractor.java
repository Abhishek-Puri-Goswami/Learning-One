package com.retailco.emailagent.nlp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls a rough hint about when the sender wants to meet out of an
 * email's free-form text, using simple keyword and pattern matching —
 * not a full natural-language date parser, and not an AI model. It only
 * recognizes a handful of common phrasings: day-of-week names,
 * morning/afternoon/evening, and explicit times like "3pm" or "10:30am."
 * <p>
 * Why it's better to find NOTHING than to guess wrong: if this class
 * can't confidently tell what the sender meant, it returns an empty hint,
 * and {@code CalendarTool} treats that as "no preference, just show me
 * whatever's available" — a safe default. Guessing wrong (say, reading
 * "not Monday" as if it meant "Monday") would actively point the search
 * in the wrong direction, which is worse than finding no hint at
 * guess for anything else. {@code calendar.CalendarTool} treats an empty
 * {@link TimeHint} as "no preference -- offer the next available slots,"
 * so under-extraction degrades gracefully; over-extraction (a wrong
 * guess) would not, which is why this class is written to fail toward
 * silence.
 */
public class DateTimeExtractor {

    private static final Pattern EXPLICIT_TIME =
            Pattern.compile("\\b(1[0-2]|0?[1-9])(:([0-5][0-9]))?\\s*(am|pm)\\b", Pattern.CASE_INSENSITIVE);

    public TimeHint extract(String text) {
        if (text == null) {
            return new TimeHint(Optional.empty(), Optional.empty(), Optional.empty());
        }
        String lower = text.toLowerCase(Locale.ROOT);

        Optional<DayOfWeek> day = findDayOfWeek(lower);
        Optional<TimeHint.PartOfDay> partOfDay = findPartOfDay(lower);
        Optional<LocalTime> explicitTime = findExplicitTime(lower);

        return new TimeHint(day, partOfDay, explicitTime);
    }

    private Optional<DayOfWeek> findDayOfWeek(String lower) {
        for (DayOfWeek d : DayOfWeek.values()) {
            String name = d.name().toLowerCase(Locale.ROOT); // e.g. "tuesday"
            if (lower.contains(name)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    private Optional<TimeHint.PartOfDay> findPartOfDay(String lower) {
        if (lower.contains("morning")) return Optional.of(TimeHint.PartOfDay.MORNING);
        if (lower.contains("afternoon")) return Optional.of(TimeHint.PartOfDay.AFTERNOON);
        if (lower.contains("evening")) return Optional.of(TimeHint.PartOfDay.EVENING);
        return Optional.empty();
    }

    private Optional<LocalTime> findExplicitTime(String lower) {
        Matcher m = EXPLICIT_TIME.matcher(lower);
        if (!m.find()) {
            return Optional.empty();
        }
        int hour = Integer.parseInt(m.group(1));
        int minute = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        boolean pm = m.group(4).equalsIgnoreCase("pm");
        int hour24 = (hour % 12) + (pm ? 12 : 0);
        return Optional.of(LocalTime.of(hour24, minute));
    }
}
