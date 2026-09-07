package com.retailco.emailagent.nlp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A deliberately conservative, rule-based (regex/keyword) date-time hint
 * extractor -- NOT a general NLP date parser. This sandbox has no LLM API
 * access, so a real "understand any phrasing of a date" system (what a
 * production version of this agent would use an LLM or a library like
 * duckling for) is out of scope; this class recognizes a bounded set of
 * common phrasings (day-of-week names, morning/afternoon/evening,
 * explicit "3pm"/"10:30am" times) and returns nothing rather than a wrong
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
