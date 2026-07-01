package io.smallrye.httpassured.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link PathSupport} — mirrors REST Assured's PathSupportTest.
 */
class PathSupportTest {

    @Test
    void shouldReturnSlashWhenFullyQualifiedUriHasNoPathButHasPort() {
        assertEquals("/", PathSupport.extractPath("http://localhost:8080"));
    }

    @Test
    void shouldReturnSlashWhenFullyQualifiedUriHasNoPathAndNoPort() {
        assertEquals("/", PathSupport.extractPath("http://localhost"));
    }

    @Test
    void shouldReturnUriAsIsWhenPathStartsWithSlash() {
        assertEquals("/api/users", PathSupport.extractPath("/api/users"));
    }

    @Test
    void shouldAddSlashToPathWhenNotStartingWithSlash() {
        assertEquals("/api/users", PathSupport.extractPath("api/users"));
    }

    @Test
    void shouldRemoveQueryParametersFromNonFullyQualifiedUri() {
        assertEquals("/api/users", PathSupport.extractPath("/api/users?page=1&size=10"));
    }

    @Test
    void shouldRemoveQueryParamsFromFullyQualifiedUriWithPath() {
        assertEquals("/api/users", PathSupport.extractPath("http://localhost:8080/api/users?page=1"));
    }

    @Test
    void shouldRemoveQueryParamsFromUriWithoutPortButWithPath() {
        assertEquals("/api/users", PathSupport.extractPath("http://localhost/api/users?page=1"));
    }

    @Test
    void shouldReturnSlashWhenPathIsUndefined() {
        assertEquals("/", PathSupport.extractPath("http://example.com"));
    }

    @Test
    void shouldReturnSlashForNullInput() {
        assertEquals("/", PathSupport.extractPath(null));
    }

    @Test
    void shouldReturnSlashForEmptyInput() {
        assertEquals("/", PathSupport.extractPath(""));
    }

    @Test
    void shouldHandlePathWithFragment() {
        assertEquals("/api/users", PathSupport.extractPath("/api/users#section"));
    }

    @Test
    void shouldExtractPathFromComplexUri() {
        assertEquals("/v1/api/resources", PathSupport.extractPath("https://api.example.com:443/v1/api/resources?key=val"));
    }
}
