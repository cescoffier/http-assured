package io.smallrye.httpassured.spi;

import io.smallrye.httpassured.http.Headers;
import io.smallrye.httpassured.http.HttpVersion;

/**
 * Raw HTTP response returned by an {@link HttpClientEngine}.
 * Contains the status code, headers, and raw body bytes.
 */
public record RawResponse(
        int statusCode,
        String statusMessage,
        Headers headers,
        byte[] body,
        long responseTimeMs,
        HttpVersion httpVersion
) {

    public RawResponse(int statusCode, String statusMessage, Headers headers, byte[] body) {
        this(statusCode, statusMessage, headers, body, -1, null);
    }

    public RawResponse(int statusCode, String statusMessage, Headers headers, byte[] body, long responseTimeMs) {
        this(statusCode, statusMessage, headers, body, responseTimeMs, null);
    }

    /**
     * Returns the body as a UTF-8 string, or empty string if body is null.
     */
    public String bodyAsString() {
        return body != null ? new String(body, java.nio.charset.StandardCharsets.UTF_8) : "";
    }

    /**
     * Returns the Content-Type header value, if present.
     */
    public java.util.Optional<String> contentType() {
        return headers.getValue("Content-Type");
    }
}
