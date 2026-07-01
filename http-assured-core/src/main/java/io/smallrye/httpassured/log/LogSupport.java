package io.smallrye.httpassured.log;

import java.util.Set;

/**
 * Package-private utilities shared by request and response loggers.
 */
final class LogSupport {

    private LogSupport() {
        // utility class
    }

    /**
     * Checks whether the given header name appears in the blacklist.
     * The blacklist set is case-insensitive (backed by a TreeSet with
     * CASE_INSENSITIVE_ORDER), so a simple contains() suffices.
     */
    static boolean isBlacklisted(String headerName, Set<String> blacklist) {
        return blacklist.contains(headerName);
    }
}
