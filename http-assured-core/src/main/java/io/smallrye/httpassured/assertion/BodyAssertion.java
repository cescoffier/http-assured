package io.smallrye.httpassured.assertion;

/**
 * Functional interface for body assertions.
 * Used by the {@code body(path, assertion)} DSL method.
 *
 * @param <T> the type of the extracted value
 */
@FunctionalInterface
public interface BodyAssertion<T> {

    /**
     * Asserts a condition on the extracted value.
     *
     * @param actual the value extracted from the response body via JsonPath
     * @param path   the JsonPath expression (for error messages)
     */
    void assertValue(T actual, String path);
}
