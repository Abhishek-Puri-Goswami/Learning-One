package com.retailco.emailagent.intent;

import com.retailco.emailagent.model.EmailMessage;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Figures out what KIND of request an email is, before deciding what to
 * do about it — like a router. This version just looks for known
 * scheduling-related keywords and phrases, rather than calling an AI
 * model to judge it. A production version could swap this class's
 * internals for a real LLM call instead, and nothing calling
 * {@link #classify} would need to change.
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
