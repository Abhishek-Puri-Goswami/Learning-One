package com.retailco.emailagent.nlp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

/**
 * A rough hint about when the sender wants to meet — NOT a confirmed
 * calendar slot, just whatever {@link DateTimeExtractor} could read out
 * of the email's text. Each field uses {@code Optional} instead of
 * allowing {@code null} — that makes it explicit and hard to miss that
 * any of these pieces of information might simply not be there.
 */
public record TimeHint(Optional<DayOfWeek> dayOfWeek, Optional<PartOfDay> partOfDay, Optional<LocalTime> explicitTime) {

    public enum PartOfDay { MORNING, AFTERNOON, EVENING }

    public boolean isEmpty() {
        return dayOfWeek.isEmpty() && partOfDay.isEmpty() && explicitTime.isEmpty();
    }
}
