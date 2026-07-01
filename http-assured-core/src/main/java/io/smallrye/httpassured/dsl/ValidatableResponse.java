package io.smallrye.httpassured.dsl;

import io.smallrye.httpassured.assertion.BodyAssertion;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.assertion.JsonSchemaOptions;
import io.smallrye.httpassured.assertion.JsonSchemaValidator;
import io.smallrye.httpassured.config.HttpAssuredConfig;
import io.smallrye.httpassured.http.HttpVersion;
import io.smallrye.httpassured.internal.SetCookieParser;
import io.smallrye.httpassured.log.ResponseLogSpec;
import io.smallrye.httpassured.log.ResponseLogger;
import io.smallrye.httpassured.mapper.jackson.JacksonObjectMapperProvider;
import io.smallrye.httpassured.path.JsonPathEvaluator;
import io.smallrye.httpassured.spec.ResponseSpec;
import io.smallrye.httpassured.spi.RawResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fluent API for validating HTTP responses.
 * <p>
 * Corresponds to the "then" phase of the given/when/then DSL.
 * All assertions use JUnit 5 — no Hamcrest.
 * </p>
 * <pre>{@code
 * .then()
 *     .statusCode(200)
 *     .header("Content-Type", "application/json")
 *     .body("name", isEqualTo("John"))
 *     .body("items", hasSize(3));
 * }</pre>
 */
public final class ValidatableResponse {

    private final RawResponse raw;
    private final HttpAssuredConfig config;
    private boolean logOnFailure = false;
    private Set<String> logBlacklist;
    private String rootPath = "";
    private String onFailMessage;

    public ValidatableResponse(RawResponse raw, HttpAssuredConfig config) {
        this.raw = raw;
        this.config = config;
        // If the instance-level flag is set, pre-activate deferred response logging
        if (config.logResponseIfValidationFails()) {
            this.logOnFailure = true;
            this.logBlacklist = config.blacklistedHeaders();
        }
    }

    // --- Logging ---

    /**
     * Returns a {@link ResponseLogSpec} to configure what to log about this response.
     * <p>
     * Supports unconditional variants ({@code all}, {@code body}, {@code headers},
     * {@code status}, {@code ifError}) and the conditional {@code ifValidationFails()}
     * which logs at ERROR level only when a subsequent assertion fails:
     * </p>
     * <pre>{@code
     * .then()
     *     .log().ifValidationFails()
     *     .statusCode(200);
     * }</pre>
     */
    public ResponseLogSpec<ValidatableResponse> log() {
        return new ResponseLogSpec<>(this, raw, config.blacklistedHeaders(),
                bl -> {
                    logOnFailure = true;
                    logBlacklist = bl;
                });
    }

    // --- Status code assertions ---

    /**
     * Asserts that the response status code equals the expected value.
     */
    public ValidatableResponse statusCode(int expected) {
        try {
            assertEquals(expected, raw.statusCode(),
                    buildMessage("Expected status code " + expected + " but was " + raw.statusCode()));
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts the full HTTP status line (e.g. "HTTP/1.1 200 OK").
     *
     * @param expected the expected status line substring
     * @return this validatable response
     */
    public ValidatableResponse statusLine(String expected) {
        try {
            String actual = raw.statusCode() + " " + raw.statusMessage();
            assertTrue(actual.contains(expected),
                    buildMessage("Expected status line to contain '" + expected + "' but was '" + actual + "'"));
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    // --- HTTP version assertion ---

    /**
     * Asserts that the response was received over the expected HTTP protocol version.
     *
     * @param expected the expected HTTP version
     * @return this validatable response
     */
    public ValidatableResponse httpVersion(HttpVersion expected) {
        try {
            assertNotNull(raw.httpVersion(),
                    buildMessage("HTTP version not available in response"));
            assertEquals(expected, raw.httpVersion(),
                    buildMessage("Expected HTTP version " + expected + " but was " + raw.httpVersion()));
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    // --- Header assertions ---

    /**
     * Asserts that a response header equals the expected value.
     */
    public ValidatableResponse header(String name, String expectedValue) {
        try {
            var actual = raw.headers().getValue(name);
            assertTrue(actual.isPresent(),
                    buildMessage("Expected header '" + name + "' to be present"));
            assertEquals(expectedValue, actual.get(),
                    buildMessage("Header '" + name + "': expected '" + expectedValue + "' but was '" + actual.get() + "'"));
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts that a response header contains the expected substring.
     */
    public ValidatableResponse headerContains(String name, String expectedSubstring) {
        try {
            var actual = raw.headers().getValue(name);
            assertTrue(actual.isPresent(),
                    buildMessage("Expected header '" + name + "' to be present"));
            assertTrue(actual.get().contains(expectedSubstring),
                    buildMessage("Header '" + name + "': expected to contain '" + expectedSubstring + "' but was '" + actual.get() + "'"));
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts that a response header is present.
     */
    public ValidatableResponse headerExists(String name) {
        try {
            assertTrue(raw.headers().hasHeader(name),
                    buildMessage("Expected header '" + name + "' to be present"));
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    // --- Cookie assertions ---

    /**
     * Asserts that a {@code Set-Cookie} response header for the named cookie
     * is present and its value equals {@code expectedValue}.
     *
     * <p>Each {@code Set-Cookie} header has the form {@code name=value; attributes}.
     * This method matches the first such header whose name equals {@code name}
     * (case-insensitive) and compares only the cookie value — attributes are ignored.
     *
     * @param name          the cookie name
     * @param expectedValue the expected cookie value
     * @return this validatable response
     */
    public ValidatableResponse cookie(String name, String expectedValue) {
        try {
            String found = SetCookieParser.findCookieValue(raw.headers().getValues("Set-Cookie"), name);
            assertNotNull(found, "Expected Set-Cookie header for cookie '" + name + "' to be present");
            assertEquals(expectedValue, found,
                    "Cookie '" + name + "': expected value '" + expectedValue + "' but was '" + found + "'");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts that a {@code Set-Cookie} response header for the named cookie satisfies
     * the given assertion.
     *
     * <p>This is the JUnit 5 equivalent of REST Assured's Hamcrest-based
     * {@code cookie("name", containsString("..."))} overload. Use any
     * {@link Assertions} factory method as the assertion:
     * <pre>{@code
     * .cookie("session_id", Assertions.containsString("abc"))
     * }</pre>
     *
     * @param name      the cookie name
     * @param assertion the assertion to apply to the cookie value
     * @return this validatable response
     */
    public ValidatableResponse cookie(String name, BodyAssertion<String> assertion) {
        try {
            String found = SetCookieParser.findCookieValue(raw.headers().getValues("Set-Cookie"), name);
            assertNotNull(found, "Expected Set-Cookie header for cookie '" + name + "' to be present");
            assertion.assertValue(found, "Set-Cookie[" + name + "]");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts that a {@code Set-Cookie} response header for the named cookie is present.
     *
     * @param name the cookie name
     * @return this validatable response
     */
    public ValidatableResponse cookieExists(String name) {
        try {
            boolean found = SetCookieParser.cookieExists(raw.headers().getValues("Set-Cookie"), name);
            assertTrue(found, "Expected Set-Cookie header for cookie '" + name + "' to be present");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts multiple response cookies in one call using alternating name/value pairs.
     *
     * <pre>{@code
     * .cookies("cookieName1", "cookieValue1", "cookieName2", "cookieValue2")
     * }</pre>
     *
     * @param nameValuePairs alternating cookie names and expected string values
     * @return this validatable response
     * @throws IllegalArgumentException if an odd number of arguments is supplied
     */
    public ValidatableResponse cookies(String... nameValuePairs) {
        try {
            if (nameValuePairs.length % 2 != 0) {
                throw new IllegalArgumentException(
                        "cookies() requires an even number of arguments (name/value pairs), got " + nameValuePairs.length);
            }
            for (int i = 0; i < nameValuePairs.length; i += 2) {
                cookie(nameValuePairs[i], nameValuePairs[i + 1]);
            }
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts multiple response cookies in one call, where each value may be either a
     * plain {@link String} (exact match) or a {@link BodyAssertion}{@code <String>}
     * (custom assertion).
     *
     * <p>This is the JUnit 5 equivalent of REST Assured's mixed Hamcrest overload:
     * <pre>{@code
     * // REST Assured (Hamcrest)
     * .cookies("n1", "v1", "n2", containsString("Value2"))
     *
     * // http-assured (JUnit 5)
     * .cookies("n1", "v1", "n2", Assertions.containsString("Value2"))
     * }</pre>
     *
     * <p>Arguments are alternating name/assertion pairs; the number of arguments
     * must be even.
     *
     * @param nameAssertionPairs alternating cookie names and expected values or assertions
     * @return this validatable response
     * @throws IllegalArgumentException if an odd number of arguments is supplied,
     *                                  or if a value element is neither a {@code String}
     *                                  nor a {@code BodyAssertion<String>}
     */
    @SuppressWarnings("unchecked")
    public ValidatableResponse cookies(Object... nameAssertionPairs) {
        try {
            if (nameAssertionPairs.length % 2 != 0) {
                throw new IllegalArgumentException(
                        "cookies() requires an even number of arguments (name/value pairs), got "
                                + nameAssertionPairs.length);
            }
            for (int i = 0; i < nameAssertionPairs.length; i += 2) {
                String name = (String) nameAssertionPairs[i];
                Object expected = nameAssertionPairs[i + 1];
                if (expected instanceof String s) {
                    cookie(name, s);
                } else if (expected instanceof BodyAssertion<?> assertion) {
                    cookie(name, (BodyAssertion<String>) assertion);
                } else {
                    throw new IllegalArgumentException(
                            "cookies() value at index " + (i + 1) + " must be a String or BodyAssertion<String>, got: "
                                    + (expected == null ? "null" : expected.getClass().getName()));
                }
            }
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    // --- Body assertions ---

    /**
     * Asserts a condition on a value extracted from the response body via JsonPath.
     *
     * @param path      JsonPath expression (auto-detected: bare path or $-prefixed)
     * @param assertion the assertion to apply to the extracted value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ValidatableResponse body(String path, BodyAssertion assertion) {
        try {
            String resolvedPath = resolvePath(path);
            String json = raw.bodyAsString();
            Object value = JsonPathEvaluator.evaluateWith(json, resolvedPath, resolveObjectMapper());
            assertion.assertValue(value, resolvedPath);
        } catch (AssertionError e) {
            logIfNeeded();
            throw wrapWithFailMessage(e);
        }
        return this;
    }

    /**
     * Asserts multiple JSON path/assertion pairs in a single call.
     *
     * @param firstPath               the first JsonPath expression
     * @param firstAssertion           the assertion for the first path
     * @param pathsAndAssertions       additional path/assertion pairs (must be even length)
     * @return this validatable response
     * @throws IllegalArgumentException if {@code pathsAndAssertions} has odd length
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ValidatableResponse body(String firstPath, BodyAssertion<?> firstAssertion, Object... pathsAndAssertions) {
        if (pathsAndAssertions.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "body() requires alternating path/assertion pairs, but got an odd number of trailing arguments: "
                            + pathsAndAssertions.length);
        }

        body(firstPath, (BodyAssertion) firstAssertion);

        for (int i = 0; i < pathsAndAssertions.length; i += 2) {
            Object pathObj = pathsAndAssertions[i];
            Object assertionObj = pathsAndAssertions[i + 1];

            if (!(pathObj instanceof String)) {
                throw new IllegalArgumentException(
                        "body() path at position " + (i + 2) + " must be a String, got: "
                                + (pathObj == null ? "null" : pathObj.getClass().getName()));
            }
            if (!(assertionObj instanceof BodyAssertion)) {
                throw new IllegalArgumentException(
                        "body() assertion at position " + (i + 3) + " must be a BodyAssertion, got: "
                                + (assertionObj == null ? "null" : assertionObj.getClass().getName()));
            }

            body((String) pathObj, (BodyAssertion) assertionObj);
        }

        return this;
    }

    /**
     * Asserts that the entire response body equals the expected string.
     */
    public ValidatableResponse bodyEquals(String expected) {
        try {
            assertEquals(expected, raw.bodyAsString(),
                    "Response body did not match expected value");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts that the response body contains the expected substring.
     */
    public ValidatableResponse bodyContains(String expected) {
        try {
            assertTrue(raw.bodyAsString().contains(expected),
                    "Response body does not contain '" + expected + "'");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    // --- JSON Schema assertions ---

    /**
     * Asserts that the entire response body conforms to the JSON Schema loaded
     * from the given classpath resource.
     *
     * <pre>{@code
     * .then()
     *     .statusCode(200)
     *     .matchesJsonSchema("schemas/user.json");
     * }</pre>
     *
     * @param classpathResource path to the JSON Schema file on the classpath
     *                          (e.g. {@code "schemas/user.json"})
     */
    public ValidatableResponse matchesJsonSchema(String classpathResource) {
        return matchesJsonSchema(classpathResource, null);
    }

    /**
     * Asserts that the entire response body conforms to the JSON Schema loaded
     * from the given classpath resource, using the given validation options.
     *
     * @param classpathResource path to the JSON Schema file on the classpath
     * @param opts              schema version and fail-fast options
     */
    public ValidatableResponse matchesJsonSchema(String classpathResource, JsonSchemaOptions opts) {
        try {
            JsonSchemaValidator.fromClasspath(classpathResource, opts)
                    .assertValue(raw.bodyAsString(), "<root>");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts that the entire response body conforms to the JSON Schema provided
     * as an inline JSON string.
     *
     * <pre>{@code
     * .then()
     *     .matchesJsonSchemaString("""
     *         {
     *           "type": "object",
     *           "required": ["id", "name"]
     *         }
     *         """);
     * }</pre>
     *
     * @param schemaJson the JSON Schema as a string
     */
    public ValidatableResponse matchesJsonSchemaString(String schemaJson) {
        return matchesJsonSchemaString(schemaJson, null);
    }

    /**
     * Asserts that the entire response body conforms to the JSON Schema provided
     * as an inline JSON string, using the given validation options.
     *
     * @param schemaJson the JSON Schema as a string
     * @param opts       schema version and fail-fast options
     */
    public ValidatableResponse matchesJsonSchemaString(String schemaJson, JsonSchemaOptions opts) {
        try {
            JsonSchemaValidator.fromString(schemaJson, opts)
                    .assertValue(raw.bodyAsString(), "<root>");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts that the entire response body conforms to the JSON Schema loaded
     * from the given {@link InputStream}.
     *
     * @param schemaStream an open {@code InputStream} providing the schema JSON
     */
    public ValidatableResponse matchesJsonSchema(InputStream schemaStream) {
        try {
            JsonSchemaValidator.fromStream(schemaStream, null)
                    .assertValue(raw.bodyAsString(), "<root>");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    /**
     * Asserts that the entire response body conforms to the JSON Schema loaded
     * from the given {@link URI}.
     *
     * @param schemaUri URI pointing at the schema (classpath, file, http, …)
     */
    public ValidatableResponse matchesJsonSchema(URI schemaUri) {
        try {
            JsonSchemaValidator.fromUri(schemaUri, null)
                    .assertValue(raw.bodyAsString(), "<root>");
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    // --- Content type assertions ---

    /**
     * Asserts the response Content-Type header.
     */
    public ValidatableResponse contentType(String expected) {
        return headerContains("Content-Type", expected);
    }

    // --- Spec application ---

    /**
     * Applies a reusable response specification.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ValidatableResponse spec(ResponseSpec spec) {
        try {
            if (spec.expectedStatusCode() != null) {
                statusCode(spec.expectedStatusCode());
            }
            for (ResponseSpec.HeaderExpectation he : spec.headerExpectations()) {
                header(he.name(), he.expectedValue());
            }
            for (ResponseSpec.BodyExpectation<?> be : spec.bodyExpectations()) {
                body(be.path(), be.assertion());
            }
            for (ResponseSpec.JsonSchemaExpectation jse : spec.jsonSchemaExpectations()) {
                matchesJsonSchema(jse.classpathResource(), jse.opts());
            }
        } catch (AssertionError e) {
            logIfNeeded();
            throw e;
        }
        return this;
    }

    // --- Chaining sugar ---

    /**
     * No-op method that returns {@code this} for fluent chaining readability.
     */
    public ValidatableResponse and() {
        return this;
    }

    /**
     * No-op method that returns {@code this} for fluent chaining readability.
     */
    public ValidatableResponse assertThat() {
        return this;
    }

    // --- Root path ---

    /**
     * Sets the root path prepended to all subsequent {@code body()} JsonPath expressions.
     *
     * @param path the root path (e.g. "store.book")
     * @return this validatable response
     */
    public ValidatableResponse rootPath(String path) {
        this.rootPath = path != null ? path : "";
        return this;
    }

    /**
     * Appends to the current root path.
     *
     * @param pathToAppend the path segment to append (joined with ".")
     * @return this validatable response
     */
    public ValidatableResponse appendRootPath(String pathToAppend) {
        if (rootPath.isEmpty()) {
            this.rootPath = pathToAppend;
        } else {
            this.rootPath = rootPath + "." + pathToAppend;
        }
        return this;
    }

    /**
     * Removes the current root path, reverting to empty.
     *
     * @return this validatable response
     */
    public ValidatableResponse detachRootPath() {
        this.rootPath = "";
        return this;
    }

    // --- Custom failure message ---

    /**
     * Sets a custom message that will be prepended to any subsequent assertion failure messages.
     *
     * @param message the custom failure message
     * @return this validatable response
     */
    public ValidatableResponse onFailMessage(String message) {
        this.onFailMessage = message;
        return this;
    }

    // --- Variadic header assertion ---

    /**
     * Asserts multiple response headers in one call using alternating name/value pairs.
     *
     * <pre>{@code
     * .headers("Content-Type", "application/json", "X-Custom", "value")
     * }</pre>
     *
     * @param firstHeaderName  the first header name
     * @param firstHeaderValue the expected value for the first header
     * @param additionalPairs  additional name/value pairs
     * @return this validatable response
     * @throws IllegalArgumentException if an odd number of additional arguments is supplied
     */
    public ValidatableResponse headers(String firstHeaderName, String firstHeaderValue, String... additionalPairs) {
        if (additionalPairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "headers() requires name/value pairs, but got an odd number of additional arguments: "
                            + additionalPairs.length);
        }
        header(firstHeaderName, firstHeaderValue);
        for (int i = 0; i < additionalPairs.length; i += 2) {
            header(additionalPairs[i], additionalPairs[i + 1]);
        }
        return this;
    }

    // --- Extract values ---

    /**
     * Extracts a value from the response body via JsonPath.
     */
    public <T> T extract(String path) {
        return JsonPathEvaluator.evaluate(raw.bodyAsString(), path);
    }

    /**
     * Deserializes the response body to the given type.
     */
    public <T> T extractAs(Class<T> type) {
        return config.objectMapper().deserialize(raw.body(), type);
    }

    /**
     * Deserializes the response body to the given type.
     */
    public <T> T extractAs(TypeReference<T> type) {
        return (T) config.objectMapper().deserialize(raw.body(), type.getType());
    }

    /**
     * Returns the underlying response for further inspection.
     */
    public Response response() {
        return new Response(raw, config);
    }

    private String resolvePath(String path) {
        if (rootPath.isEmpty() || path.startsWith("$")) {
            return path;
        }
        return rootPath + "." + path;
    }

    private String buildMessage(String message) {
        if (onFailMessage != null) {
            return onFailMessage + ": " + message;
        }
        return message;
    }

    private AssertionError wrapWithFailMessage(AssertionError e) {
        if (onFailMessage != null) {
            return new AssertionError(onFailMessage + ": " + e.getMessage(), e);
        }
        return e;
    }

    private void logIfNeeded() {
        if (logOnFailure) {
            Set<String> bl = logBlacklist != null ? logBlacklist : config.blacklistedHeaders();
            ResponseLogger.logError(raw, bl);
        }
    }

    private ObjectMapper resolveObjectMapper() {
        if (config.objectMapper() instanceof JacksonObjectMapperProvider jop) {
            return jop.objectMapper();
        }
        return null;
    }
}
