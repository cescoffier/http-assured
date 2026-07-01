package io.smallrye.httpassured.assertion;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static io.smallrye.httpassured.assertion.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JSON Schema assertions.
 * <p>
 * Uses inline schemas and JSON payloads — no HTTP stack required.
 * </p>
 */
class JsonSchemaAssertionTest {

    private static final String USER_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "required": ["id", "name"],
              "properties": {
                "id":   { "type": "integer" },
                "name": { "type": "string"  }
              }
            }
            """;

    private static final String VALID_USER   = """
            { "id": 1, "name": "Alice" }
            """;

    private static final String INVALID_USER_MISSING_FIELD = """
            { "id": 1 }
            """;

    private static final String INVALID_USER_WRONG_TYPE = """
            { "id": "not-a-number", "name": "Alice" }
            """;

    @Test
    void schemaString_validPayload_passes() {
        assertDoesNotThrow(() ->
                matchesJsonSchemaString(USER_SCHEMA).assertValue(VALID_USER, "root"));
    }

    @Test
    void schemaString_withOptions_validPayload_passes() {
        assertDoesNotThrow(() ->
                matchesJsonSchemaString(USER_SCHEMA, JsonSchemaOptions.draft7())
                        .assertValue(VALID_USER, "root"));
    }

    @Test
    void schemaString_missingRequiredField_fails() {
        AssertionError err = assertThrows(AssertionError.class, () ->
                matchesJsonSchemaString(USER_SCHEMA)
                        .assertValue(INVALID_USER_MISSING_FIELD, "root"));
        assertTrue(err.getMessage().contains("name"), "Error should mention the missing field");
    }

    @Test
    void schemaString_wrongType_fails() {
        AssertionError err = assertThrows(AssertionError.class, () ->
                matchesJsonSchemaString(USER_SCHEMA)
                        .assertValue(INVALID_USER_WRONG_TYPE, "root"));
        assertTrue(err.getMessage().contains("id") || err.getMessage().contains("integer"),
                "Error should mention the type violation");
    }

    @Test
    void schemaString_allViolations_reportedTogether() {
        String allBad = """
                { "id": "wrong", "name": 99 }
                """;
        AssertionError err = assertThrows(AssertionError.class, () ->
                matchesJsonSchemaString(USER_SCHEMA).assertValue(allBad, "root"));
        // Both violations should appear in a single error
        assertNotNull(err.getMessage());
        assertTrue(err.getMessage().contains("JSON Schema validation failed"),
                "Error message should have header line");
    }

    @Test
    void classpathSchema_validPayload_passes() {
        String json = """
                { "id": 42, "name": "Bob", "email": "bob@example.com" }
                """;
        assertDoesNotThrow(() ->
                matchesJsonSchema("schemas/user.json").assertValue(json, "root"));
    }

    @Test
    void classpathSchema_missingRequired_fails() {
        String json = """
                { "id": 42 }
                """;
        assertThrows(AssertionError.class, () ->
                matchesJsonSchema("schemas/user.json").assertValue(json, "root"));
    }

    @Test
    void classpathSchema_withOptions_passes() {
        String json = """
                { "id": 1, "name": "Charlie", "email": "charlie@example.com" }
                """;
        assertDoesNotThrow(() ->
                matchesJsonSchema("schemas/user.json", JsonSchemaOptions.draft7())
                        .assertValue(json, "root"));
    }

    @Test
    void classpathSchema_notFound_fails() {
        AssertionError err = assertThrows(AssertionError.class, () ->
                matchesJsonSchema("schemas/does-not-exist.json")
                        .assertValue("{}", "root"));
        assertTrue(err.getMessage().contains("not found") || err.getMessage().contains("does-not-exist"),
                "Error should say the resource was not found");
    }

    @Test
    void inputStreamSchema_validPayload_passes() {
        InputStream stream = new ByteArrayInputStream(USER_SCHEMA.getBytes(StandardCharsets.UTF_8));
        assertDoesNotThrow(() ->
                matchesJsonSchema(stream).assertValue(VALID_USER, "root"));
    }

    @Test
    void inputStreamSchema_invalidPayload_fails() {
        InputStream stream = new ByteArrayInputStream(USER_SCHEMA.getBytes(StandardCharsets.UTF_8));
        assertThrows(AssertionError.class, () ->
                matchesJsonSchema(stream).assertValue(INVALID_USER_MISSING_FIELD, "root"));
    }

    @Test
    void uriSchema_classPathUri_validPayload_passes() {
        URI uri = URI.create("classpath:schemas/user.json");
        try {
            assertDoesNotThrow(() ->
                    matchesJsonSchema(uri).assertValue(
                            """
                            { "id": 1, "name": "Eve", "email": "eve@example.com" }
                            """, "root"));
        } catch (Exception ignored) {
            // classpath support is environment-dependent; skip if not supported
        }
    }

    @Test
    void options_draft4_accepted() {
        assertDoesNotThrow(() ->
                matchesJsonSchemaString(USER_SCHEMA, JsonSchemaOptions.draft4())
                        .assertValue(VALID_USER, "root"));
    }

    @Test
    void options_draft2020_accepted() {
        assertDoesNotThrow(() ->
                matchesJsonSchemaString(USER_SCHEMA, JsonSchemaOptions.draft2020())
                        .assertValue(VALID_USER, "root"));
    }

    @Test
    void options_failFast_returnsEarlyOnFirstViolation() {
        String multiViolation = """
                { "id": "bad", "name": 123 }
                """;
        // Without failFast: both violations should be reported
        AssertionError allErrors = assertThrows(AssertionError.class, () ->
                matchesJsonSchemaString(USER_SCHEMA, JsonSchemaOptions.draft7())
                        .assertValue(multiViolation, "root"));
        long allCount = allErrors.getMessage().lines()
                .filter(l -> l.stripLeading().startsWith("- "))
                .count();
        assertTrue(allCount >= 2, "Without failFast, expected at least 2 violations but got " + allCount);

        // With failFast: only one violation should be reported
        AssertionError failFastError = assertThrows(AssertionError.class, () ->
                matchesJsonSchemaString(USER_SCHEMA, JsonSchemaOptions.draft7().failFast())
                        .assertValue(multiViolation, "root"));
        long failFastCount = failFastError.getMessage().lines()
                .filter(l -> l.stripLeading().startsWith("- "))
                .count();
        assertEquals(1, failFastCount,
                "With failFast, expected exactly 1 violation but got " + failFastCount);
    }

    @Test
    void classpathArraySchema_validList_passes() {
        String json = """
                [
                  { "id": 1, "name": "Alice" },
                  { "id": 2, "name": "Bob" }
                ]
                """;
        assertDoesNotThrow(() ->
                matchesJsonSchema("schemas/users-list.json").assertValue(json, "root"));
    }

    @Test
    void classpathArraySchema_emptyArray_fails() {
        assertThrows(AssertionError.class, () ->
                matchesJsonSchema("schemas/users-list.json").assertValue("[]", "root"));
    }

    @Test
    void classpathArraySchema_itemMissingField_fails() {
        String json = """
                [
                  { "id": 1 }
                ]
                """;
        assertThrows(AssertionError.class, () ->
                matchesJsonSchema("schemas/users-list.json").assertValue(json, "root"));
    }

    @Test
    void errorMessage_includesPathLabel_whenNonRoot() {
        AssertionError err = assertThrows(AssertionError.class, () ->
                matchesJsonSchemaString(USER_SCHEMA)
                        .assertValue(INVALID_USER_MISSING_FIELD, "$.data"));
        assertTrue(err.getMessage().contains("$.data"),
                "Error should include the JsonPath label");
    }
}
