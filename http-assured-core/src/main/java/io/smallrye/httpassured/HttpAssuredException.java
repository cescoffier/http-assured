package io.smallrye.httpassured;

/**
 * Base exception for all http-assured errors.
 */
public class HttpAssuredException extends RuntimeException {

    public HttpAssuredException(String message) {
        super(message);
    }

    public HttpAssuredException(String message, Throwable cause) {
        super(message, cause);
    }
}
