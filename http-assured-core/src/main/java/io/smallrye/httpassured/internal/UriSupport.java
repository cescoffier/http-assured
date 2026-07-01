package io.smallrye.httpassured.internal;

/**
 * Utility for URI manipulation.
 */
public final class UriSupport {

    private UriSupport() {}

    /**
     * Injects or replaces the port in a URI.
     * <p>
     * If the URI already contains a port (e.g., {@code http://host:8080/path}),
     * it is replaced with the given port. If no port is present, it is inserted
     * between the host and the path.
     * </p>
     *
     * @param uri the URI string (e.g., {@code http://localhost/path})
     * @param port the port to inject
     * @return the URI with the port injected or replaced
     */
    public static String injectPort(String uri, int port) {
        if (uri == null || uri.isEmpty() || port <= 0) {
            return uri;
        }

        // Check if URI has scheme (http/https/ws/wss)
        if (!uri.matches("^[a-z]+://.*")) {
            return uri;
        }

        // Replace existing port
        if (uri.matches("^[a-z]+://[^/:]+:\\d+.*")) {
            return uri.replaceFirst(":\\d+", ":" + port);
        }

        // Inject port between host and path (or at end if no path)
        int schemeEnd = uri.indexOf("://") + 3;
        int pathStart = uri.indexOf('/', schemeEnd);
        if (pathStart == -1) {
            return uri + ":" + port;
        } else {
            return uri.substring(0, pathStart) + ":" + port + uri.substring(pathStart);
        }
    }
}
