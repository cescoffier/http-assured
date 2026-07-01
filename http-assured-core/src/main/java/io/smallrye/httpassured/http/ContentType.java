package io.smallrye.httpassured.http;

import java.nio.charset.Charset;

/**
 * Common content types for HTTP requests and responses.
 */
public enum ContentType {

    JSON("application/json"),
    XML("application/xml"),
    TEXT("text/plain"),
    HTML("text/html"),
    FORM_URL_ENCODED("application/x-www-form-urlencoded"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    BINARY("application/octet-stream");

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    /**
     * Returns the MIME type string.
     */
    public String value() {
        return value;
    }

    /**
     * Returns the content type with the given charset appended.
     *
     * @param charset the charset name (e.g. "UTF-8")
     * @return the content type with charset, e.g. "application/json; charset=UTF-8"
     */
    public String withCharset(String charset) {
        if (charset == null || charset.isEmpty()) {
            return value;
        }
        return value + "; charset=" + charset;
    }

    /**
     * Returns the content type with the given Java charset appended.
     *
     * @param charset the charset
     * @return the content type with charset
     */
    public String withCharset(Charset charset) {
        if (charset == null) {
            return value;
        }
        return withCharset(charset.name());
    }

    /**
     * Checks if the given content type string matches this content type (case-insensitive).
     * The comparison ignores parameters like charset.
     *
     * @param contentType the content type string to check
     * @return true if the base MIME type matches
     */
    public boolean matches(String contentType) {
        if (contentType == null) {
            return false;
        }
        // Extract just the MIME type (before any parameters like charset)
        String baseType = contentType.contains(";")
                ? contentType.substring(0, contentType.indexOf(';')).trim()
                : contentType.trim();
        return value.equalsIgnoreCase(baseType);
    }

    /**
     * Finds a ContentType from a content type string.
     * Matches case-insensitively against the base MIME type.
     *
     * @param contentType the content type string
     * @return the matching ContentType, or null if none matches
     */
    public static ContentType fromString(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return null;
        }
        for (ContentType ct : values()) {
            if (ct.matches(contentType)) {
                return ct;
            }
        }
        return null;
    }

    /**
     * Extracts just the content type without charset or other parameters.
     *
     * @param contentTypeWithParams the full content-type header value, e.g. "application/json; charset=UTF-8"
     * @return the base content type, e.g. "application/json"
     */
    public static String withoutCharset(String contentTypeWithParams) {
        if (contentTypeWithParams == null || contentTypeWithParams.isEmpty()) {
            return contentTypeWithParams;
        }
        int semicolonIndex = contentTypeWithParams.indexOf(';');
        if (semicolonIndex == -1) {
            return contentTypeWithParams;
        }
        return contentTypeWithParams.substring(0, semicolonIndex).trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
