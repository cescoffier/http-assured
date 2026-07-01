package io.smallrye.httpassured.internal;

import java.net.URI;

/**
 * Utility for extracting and normalizing paths from URIs.
 */
public final class PathSupport {

    private PathSupport() {}

    /**
     * Extracts the path portion from a URI string, stripping query parameters.
     * <p>
     * For fully-qualified URIs, returns the path component.
     * For relative paths, returns the path with a leading slash.
     * </p>
     *
     * @param uri the URI string
     * @return the extracted path, always starting with "/"
     */
    public static String extractPath(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "/";
        }

        // Check if fully qualified (has scheme)
        if (uri.contains("://")) {
            try {
                URI parsed = new URI(uri);
                String path = parsed.getPath();
                if (path == null || path.isEmpty()) {
                    return "/";
                }
                return path;
            } catch (Exception e) {
                // Fall through to manual parsing
            }
        }

        // Strip query parameters and fragments
        String path = uri;
        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex != -1) {
            path = path.substring(0, fragmentIndex);
        }
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            path = path.substring(0, queryIndex);
        }

        // Ensure leading slash
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }
}
