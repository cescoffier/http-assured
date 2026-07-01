package io.smallrye.httpassured.internal;

import io.smallrye.httpassured.http.Cookie;

import java.util.List;

/**
 * Package-private utility for parsing Set-Cookie response headers.
 */
public final class SetCookieParser {

    private SetCookieParser() {
        // utility class
    }

    /**
     * Finds and returns the raw value of a cookie with the given name from Set-Cookie headers.
     *
     * @param setCookieHeaders list of Set-Cookie header values
     * @param cookieName       the cookie name to search for (case-insensitive)
     * @return the cookie value, or {@code null} if not found
     */
    public static String findCookieValue(List<String> setCookieHeaders, String cookieName) {
        for (String header : setCookieHeaders) {
            String nameValue = extractNameValue(header);
            int eq = nameValue.indexOf('=');
            if (eq > 0) {
                String name = nameValue.substring(0, eq).trim();
                if (name.equalsIgnoreCase(cookieName)) {
                    return nameValue.substring(eq + 1).trim();
                }
            }
        }
        return null;
    }

    /**
     * Finds and parses a full Cookie object with the given name from Set-Cookie headers.
     *
     * @param setCookieHeaders list of Set-Cookie header values
     * @param cookieName       the cookie name to search for (case-insensitive)
     * @return the parsed cookie, or {@code null} if not found
     */
    public static Cookie findCookie(List<String> setCookieHeaders, String cookieName) {
        for (String header : setCookieHeaders) {
            String nameValue = extractNameValue(header);
            int eq = nameValue.indexOf('=');
            if (eq > 0 && nameValue.substring(0, eq).trim().equalsIgnoreCase(cookieName)) {
                return parseCookie(cookieName, header);
            }
        }
        return null;
    }

    /**
     * Checks if a cookie with the given name exists in Set-Cookie headers.
     *
     * @param setCookieHeaders list of Set-Cookie header values
     * @param cookieName       the cookie name to search for (case-insensitive)
     * @return {@code true} if the cookie exists, {@code false} otherwise
     */
    public static boolean cookieExists(List<String> setCookieHeaders, String cookieName) {
        for (String header : setCookieHeaders) {
            String nameValue = extractNameValue(header);
            int eq = nameValue.indexOf('=');
            if (eq > 0 && nameValue.substring(0, eq).trim().equalsIgnoreCase(cookieName)) {
                return true;
            }
        }
        return false;
    }

    private static String extractNameValue(String setCookieHeader) {
        return setCookieHeader.contains(";")
                ? setCookieHeader.substring(0, setCookieHeader.indexOf(';')).trim()
                : setCookieHeader.trim();
    }

    private static Cookie parseCookie(String name, String setCookieHeader) {
        String[] parts = setCookieHeader.split(";");
        String value = "";
        int eq = parts[0].indexOf('=');
        if (eq > 0) {
            value = parts[0].substring(eq + 1).trim();
        }

        Cookie.Builder builder = Cookie.builder(name, value);
        for (int i = 1; i < parts.length; i++) {
            String attr = parts[i].trim();
            int aeq = attr.indexOf('=');
            String aName = aeq > 0 ? attr.substring(0, aeq).trim() : attr.trim();
            String aValue = aeq > 0 ? attr.substring(aeq + 1).trim() : "";

            switch (aName.toLowerCase()) {
                case "domain" -> builder.domain(aValue);
                case "path" -> builder.path(aValue);
                case "max-age" -> {
                    try { builder.maxAge(Long.parseLong(aValue)); } catch (NumberFormatException ignored) {}
                }
                case "secure" -> builder.secure(true);
                case "httponly" -> builder.httpOnly(true);
                case "samesite" -> builder.sameSite(aValue);
            }
        }
        return builder.build();
    }
}
