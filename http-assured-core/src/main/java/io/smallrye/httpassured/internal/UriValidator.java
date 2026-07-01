package io.smallrye.httpassured.internal;

import java.net.URI;

/**
 * Validates URI strings.
 */
public final class UriValidator {

    private UriValidator() {}

    /**
     * Checks if the given string is a valid, fully-qualified URI
     * (i.e., has both a scheme and a host).
     *
     * @param uri the URI string to validate
     * @return true if valid with scheme and host, false otherwise
     */
    public static boolean isValid(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        try {
            URI parsed = new URI(uri);
            return parsed.getScheme() != null && parsed.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
