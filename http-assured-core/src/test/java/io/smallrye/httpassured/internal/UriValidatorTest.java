package io.smallrye.httpassured.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link UriValidator} — mirrors REST Assured's UriValidatorTest.
 */
class UriValidatorTest {

    @Test
    void shouldReturnFalseWhenUriIsEmpty() {
        assertFalse(UriValidator.isValid(""));
    }

    @Test
    void shouldReturnFalseWhenUriIsNull() {
        assertFalse(UriValidator.isValid(null));
    }

    @Test
    void shouldReturnFalseWhenUriIsBlank() {
        assertFalse(UriValidator.isValid("   "));
    }

    @Test
    void shouldReturnFalseWhenUriHasNoScheme() {
        assertFalse(UriValidator.isValid("localhost:8080/api"));
    }

    @Test
    void shouldReturnFalseWhenUriHasNoHost() {
        assertFalse(UriValidator.isValid("http://"));
    }

    @Test
    void shouldReturnFalseWhenUriIsMalformed() {
        assertFalse(UriValidator.isValid("not a uri at all"));
    }

    @Test
    void shouldReturnTrueWhenUriContainsSchemeAndHost() {
        assertTrue(UriValidator.isValid("http://localhost"));
    }

    @Test
    void shouldReturnTrueWhenUriContainsSchemeHostAndPort() {
        assertTrue(UriValidator.isValid("http://localhost:8080"));
    }

    @Test
    void shouldReturnTrueWhenUriContainsSchemeHostPortAndPath() {
        assertTrue(UriValidator.isValid("http://localhost:8080/api/users"));
    }

    @Test
    void shouldReturnTrueForHttps() {
        assertTrue(UriValidator.isValid("https://example.com"));
    }

    @Test
    void shouldReturnFalseForJustScheme() {
        assertFalse(UriValidator.isValid("http:"));
    }

    @Test
    void shouldReturnTrueForUriWithQueryParams() {
        assertTrue(UriValidator.isValid("http://localhost:8080/api?page=1&size=10"));
    }
}
