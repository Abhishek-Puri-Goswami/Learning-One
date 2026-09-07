package com.retailco.emailagent.intent;

import com.retailco.emailagent.model.EmailMessage;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rule-based intent classification (keyword/phrase matching), not an LLM
 * call -- disclosed as a stand-in the same way L2's UC3 live-data intent
 * routing is: this sandbox has no LLM API access, so the routing logic is
 * a deterministic, testable substitute for what a real system would ask an
 * LLM to classify. A production version would swap this class's internals
 * for a prompted LLM call behind the same {@link #classify} signature.
 */
public class IntentClassifier {

    private static final Set<String> SCHEDULING_KEYWORDS = Set.of(
            "schedule", "scheduling", "meeting", "meet", "call", "sync",
            "availability", "available", "book a time", "find time", "calendar"
    );

    private static final Pattern QUESTION_ABOUT_TIME =
            Pattern.compile("\\b(when|what time|which day)\\b.*\\?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public EmailIntent classify(EmailMessage email) {
        String haystack = (nullToEmpty(email.subject()) + " " + nullToEmpty(email.body())).toLowerCase();
        boolean hasSchedulingKeyword = SCHEDULING_KEYWORDS.stream().anyMatch(haystack::contains);
        boolean asksAboutTiming = QUESTION_ABOUT_TIME.matcher(haystack).find();
        return (hasSchedulingKeyword || asksAboutTiming) ? EmailIntent.SCHEDULING_REQUEST : EmailIntent.OTHER;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
