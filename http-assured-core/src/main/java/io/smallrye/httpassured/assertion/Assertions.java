package io.smallrye.httpassured.assertion;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assertion helpers for use in {@code body(path, assertion)} calls.
 * <p>
 * These wrap JUnit 5 assertions — no Hamcrest dependency.
 * </p>
 * <pre>{@code
 * .then()
 *     .body("name", isEqualTo("John"))
 *     .body("age", satisfies(age -> age > 18))
 *     .body("items", hasSize(3))
 *     .body("items[*].price", allMatch(p -> p > 0))
 * }</pre>
 */
public final class Assertions {

    private Assertions() {}

    /**
     * Asserts that the extracted value equals the expected value.
     */
    public static <T> BodyAssertion<T> isEqualTo(T expected) {
        return (actual, path) -> assertEquals(expected, actual,
                "Body path '" + path + "': expected " + expected + " but was " + actual);
    }

    /**
     * Asserts that the extracted value does not equal the given value.
     */
    public static <T> BodyAssertion<T> isNotEqualTo(T unexpected) {
        return (actual, path) -> assertNotEquals(unexpected, actual,
                "Body path '" + path + "': expected value to differ from " + unexpected);
    }

    /**
     * Asserts that the extracted value is null.
     */
    public static <T> BodyAssertion<T> isNull() {
        return (actual, path) -> assertNull(actual,
                "Body path '" + path + "': expected null but was " + actual);
    }

    /**
     * Asserts that the extracted value is not null.
     */
    public static <T> BodyAssertion<T> isNotNull() {
        return (actual, path) -> assertNotNull(actual,
                "Body path '" + path + "': expected non-null value");
    }

    /**
     * Asserts that the extracted value satisfies the given predicate.
     */
    @SuppressWarnings("unchecked")
    public static <T> BodyAssertion<T> satisfies(Predicate<T> predicate) {
        return (actual, path) -> assertTrue(predicate.test(actual),
                "Body path '" + path + "': value " + actual + " did not satisfy predicate");
    }

    /**
     * Asserts that the extracted collection has the expected size.
     */
    @SuppressWarnings("unchecked")
    public static <T> BodyAssertion<T> hasSize(int expectedSize) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected collection but was null");
            if (actual instanceof Collection<?> collection) {
                assertEquals(expectedSize, collection.size(),
                        "Body path '" + path + "': expected size " + expectedSize + " but was " + collection.size());
            } else {
                fail("Body path '" + path + "': expected a collection but got " + actual.getClass().getName());
            }
        };
    }

    /**
     * Asserts that all elements in the extracted collection match the given predicate.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> BodyAssertion<T> allMatch(Predicate predicate) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected collection but was null");
            assertInstanceOf(Collection.class, actual,
                    "Body path '" + path + "': expected a collection but got " + actual.getClass().getName());
            Collection<?> collection = (Collection<?>) actual;
            for (Object element : collection) {
                assertTrue(predicate.test(element),
                        "Body path '" + path + "': element " + element + " did not match predicate");
            }
        };
    }

    /**
     * Asserts that any element in the extracted collection matches the given predicate.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> BodyAssertion<T> anyMatch(Predicate predicate) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected collection but was null");
            assertInstanceOf(Collection.class, actual,
                    "Body path '" + path + "': expected a collection but got " + actual.getClass().getName());
            Collection<?> collection = (Collection<?>) actual;
            boolean found = false;
            for (Object element : collection) {
                if (predicate.test(element)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found,
                    "Body path '" + path + "': no element matched the predicate");
        };
    }

    /**
     * Asserts that the extracted collection contains the expected element.
     */
    public static <T> BodyAssertion<T> contains(Object expected) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected collection but was null");
            assertInstanceOf(Collection.class, actual,
                    "Body path '" + path + "': expected a collection but got " + actual.getClass().getName());
            assertTrue(((Collection<?>) actual).contains(expected),
                    "Body path '" + path + "': collection does not contain " + expected);
        };
    }

    /**
     * Asserts that the extracted collection contains all expected elements.
     */
    public static <T> BodyAssertion<T> containsAll(Object... expected) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected collection but was null");
            assertInstanceOf(Collection.class, actual,
                    "Body path '" + path + "': expected a collection but got " + actual.getClass().getName());
            Collection<?> collection = (Collection<?>) actual;
            for (Object exp : expected) {
                assertTrue(collection.contains(exp),
                        "Body path '" + path + "': collection does not contain " + exp);
            }
        };
    }

    /**
     * Asserts that the extracted string contains the expected substring.
     */
    public static BodyAssertion<String> containsString(String expected) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected string but was null");
            assertTrue(actual.contains(expected),
                    "Body path '" + path + "': '" + actual + "' does not contain '" + expected + "'");
        };
    }

    /**
     * Asserts that the extracted string matches the expected regex.
     */
    public static BodyAssertion<String> matchesPattern(String regex) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected string but was null");
            assertTrue(actual.matches(regex),
                    "Body path '" + path + "': '" + actual + "' does not match pattern '" + regex + "'");
        };
    }

    /**
     * Asserts that the extracted comparable value is greater than the expected value.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Comparable> BodyAssertion<T> greaterThan(T expected) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected comparable but was null");
            assertTrue(actual.compareTo(expected) > 0,
                    "Body path '" + path + "': expected > " + expected + " but was " + actual);
        };
    }

    /**
     * Asserts that the extracted comparable value is less than the expected value.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Comparable> BodyAssertion<T> lessThan(T expected) {
        return (actual, path) -> {
            assertNotNull(actual, "Body path '" + path + "': expected comparable but was null");
            assertTrue(actual.compareTo(expected) < 0,
                    "Body path '" + path + "': expected < " + expected + " but was " + actual);
        };
    }

    /**
     * Asserts that the extracted JSON string conforms to the schema loaded from
     * a classpath resource.
     *
     * <pre>{@code
     * .body("$.data", Assertions.matchesJsonSchema("schemas/user.json"))
     * }</pre>
     *
     * @param classpathResource path to the JSON Schema file on the classpath
     *                          (e.g. {@code "schemas/user.json"})
     */
    public static BodyAssertion<Object> matchesJsonSchema(String classpathResource) {
        return JsonSchemaValidator.fromClasspath(classpathResource, null);
    }

    /**
     * Asserts that the extracted JSON string conforms to the schema loaded from
     * a classpath resource, using the given validation options.
     *
     * @param classpathResource path to the JSON Schema file on the classpath
     * @param opts              schema version and fail-fast options
     */
    public static BodyAssertion<Object> matchesJsonSchema(String classpathResource, JsonSchemaOptions opts) {
        return JsonSchemaValidator.fromClasspath(classpathResource, opts);
    }

    /**
     * Asserts that the extracted JSON string conforms to the schema provided
     * as an inline JSON string.
     *
     * <pre>{@code
     * .body("$.data", Assertions.matchesJsonSchemaString("""
     *     { "type": "object", "required": ["id"] }
     *     """))
     * }</pre>
     *
     * @param schemaJson the JSON Schema as a string
     */
    public static BodyAssertion<Object> matchesJsonSchemaString(String schemaJson) {
        return JsonSchemaValidator.fromString(schemaJson, null);
    }

    /**
     * Asserts that the extracted JSON string conforms to the schema provided
     * as an inline JSON string, using the given validation options.
     *
     * @param schemaJson the JSON Schema as a string
     * @param opts       schema version and fail-fast options
     */
    public static BodyAssertion<Object> matchesJsonSchemaString(String schemaJson, JsonSchemaOptions opts) {
        return JsonSchemaValidator.fromString(schemaJson, opts);
    }

    /**
     * Asserts that the extracted JSON string conforms to the schema loaded from
     * the given {@link InputStream}.
     *
     * @param schemaStream an open {@code InputStream} providing the schema JSON
     */
    public static BodyAssertion<Object> matchesJsonSchema(InputStream schemaStream) {
        return JsonSchemaValidator.fromStream(schemaStream, null);
    }

    /**
     * Asserts that the extracted JSON string conforms to the schema loaded from
     * the given {@link URI}.
     *
     * @param schemaUri URI pointing at the schema (classpath, file, http, …)
     */
    public static BodyAssertion<Object> matchesJsonSchema(URI schemaUri) {
        return JsonSchemaValidator.fromUri(schemaUri, null);
    }
}
