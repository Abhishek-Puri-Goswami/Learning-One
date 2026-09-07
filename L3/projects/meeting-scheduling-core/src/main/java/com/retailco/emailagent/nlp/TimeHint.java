package com.retailco.emailagent.nlp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

/**
 * A parsed hint about when the sender wants to meet -- not a resolved
 * calendar slot (that's {@code calendar.CalendarTool}'s job), just what
 * {@link DateTimeExtractor} could read out of free text. Any field may be
 * absent; the extractor is intentionally conservative (see its own
 * Javadoc) rather than guessing.
 */
public record TimeHint(Optional<DayOfWeek> dayOfWeek, Optional<PartOfDay> partOfDay, Optional<LocalTime> explicitTime) {

    public enum PartOfDay { MORNING, AFTERNOON, EVENING }

    public boolean isEmpty() {
        return dayOfWeek.isEmpty() && partOfDay.isEmpty() && explicitTime.isEmpty();
    }
}
