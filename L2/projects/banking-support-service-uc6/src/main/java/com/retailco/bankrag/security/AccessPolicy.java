package com.retailco.bankrag.security;

import java.util.List;
import java.util.Set;

/**
 * Answers exactly one question, in exactly one place: "given this
 * subject and these roles, can they access this customer's data?" It
 * returns a {@code Decision} — either {@code Allowed} or {@code Denied} —
 * which forces every caller to explicitly handle both outcomes instead
 * of accidentally forgetting one.
 * <p>
 * Centralizing this check matters because the permission matrix below is
 * only trustworthy if there is exactly ONE place that implements it — if
 * every caller re-implemented "is this admin, or support staff, or the
 * customer themself" inline, the matrix could quietly drift out of sync
 * with what the code actually does. {@code BankingToolService} calls
 * {@code AccessPolicy.evaluate(...)} for every request before returning
 * any data.
 *
 * Permission matrix:
 * <pre>
 *   Role           | Own data | Other customer's data
 *   ---------------|----------|------------------------
 *   CUSTOMER       | allow    | deny
 *   SUPPORT_AGENT  | allow    | allow (read-only; masked, same as UC3)
 *   ADMIN          | allow    | allow
 *   (no known role)| deny     | deny
 * </pre>
 */
public final class AccessPolicy {

    private AccessPolicy() {
    }

    public sealed interface Decision permits Allowed, Denied {
        String reason();
    }

    public record Allowed(String reason, Role grantingRole, boolean elevated) implements Decision {
    }

    public record Denied(String reason) implements Decision {
    }

    public static Decision evaluate(String subject, List<String> roleStrings, String requestedCustomerId) {
        Set<Role> roles = parseRoles(roleStrings);

        boolean isSelf = subject != null && subject.equals(requestedCustomerId);
        if (isSelf) {
            // A customer can always see their own data, no matter what
            // role their token carries — even a token with no recognized
            // role at all can still act as its own customer.
            return new Allowed("subject matches requested customer", roles.contains(Role.CUSTOMER) ? Role.CUSTOMER : firstOrCustomer(roles), false);
        }

        if (roles.contains(Role.ADMIN)) {
            return new Allowed("ADMIN role grants cross-customer access", Role.ADMIN, true);
        }
        if (roles.contains(Role.SUPPORT_AGENT)) {
            return new Allowed("SUPPORT_AGENT role grants read-only cross-customer access", Role.SUPPORT_AGENT, true);
        }
        return new Denied("subject '" + subject + "' holds no role that permits access to customer '" + requestedCustomerId + "'");
    }

    private static Role firstOrCustomer(Set<Role> roles) {
        return roles.isEmpty() ? Role.CUSTOMER : roles.iterator().next();
    }

    /**
     * A role string this version of the service doesn't recognize is
     * quietly ignored rather than rejected outright — it simply grants no
     * extra access, since {@link #evaluate} only ever expands access for
     * roles it actually knows about.
     */
    private static Set<Role> parseRoles(List<String> roleStrings) {
        if (roleStrings == null) return Set.of();
        return roleStrings.stream()
                .map(AccessPolicy::tryParse)
                .filter(r -> r != null)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static Role tryParse(String s) {
        try {
            return Role.valueOf(s);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
