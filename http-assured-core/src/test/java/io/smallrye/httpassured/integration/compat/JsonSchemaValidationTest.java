package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.JsonSchemaOptions;
import io.smallrye.httpassured.spec.ResponseSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.smallrye.httpassured.assertion.Assertions.matchesJsonSchema;
import static io.smallrye.httpassured.assertion.Assertions.matchesJsonSchemaString;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for JSON Schema validation against a WireMock server.
 * <p>
 * Mirrors the REST Assured JSON Schema validation examples from
 * <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#json-schema-validation">
 * REST Assured Usage — JSON Schema Validation</a>.
 * </p>
 */
@WireMockTest
class JsonSchemaValidationTest {

    private static final String USER_JSON = """
            {"id":1,"name":"Alice","email":"alice@example.com"}
            """.strip();

    private static final String USERS_JSON = """
            [{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]
            """.strip();

    private static final String INLINE_USER_SCHEMA = """
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

    private HttpAssured client;

    @BeforeEach
    void setup(WireMockRuntimeInfo wm) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wm.getHttpPort())
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
    }

    @Nested
    class ClasspathSchema {

        @Test
        void singleUser_conformsToSchema() {
            stubFor(get("/users/1")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(USER_JSON)));

            client.when().get("/users/1")
                    .then()
                    .statusCode(200)
                    .matchesJsonSchema("schemas/user.json");
        }

        @Test
        void userList_conformsToArraySchema() {
            stubFor(get("/users")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(USERS_JSON)));

            client.when().get("/users")
                    .then()
                    .statusCode(200)
                    .matchesJsonSchema("schemas/users-list.json");
        }

        @Test
        void invalidBody_throwsAssertionError() {
            String missingEmail = """
                    {"id":1,"name":"Alice"}
                    """.strip();
            stubFor(get("/users/bad")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(missingEmail)));

            assertThrows(AssertionError.class, () ->
                    client.when().get("/users/bad")
                            .then()
                            .matchesJsonSchema("schemas/user.json"));
        }

        @Test
        void singleUser_withDraft7Options_passes() {
            stubFor(get("/users/1")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(USER_JSON)));

            client.when().get("/users/1")
                    .then()
                    .matchesJsonSchema("schemas/user.json", JsonSchemaOptions.draft7());
        }
    }

    @Nested
    class InlineSchema {

        @Test
        void validPayload_passes() {
            stubFor(get("/users/1")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(USER_JSON)));

            client.when().get("/users/1")
                    .then()
                    .statusCode(200)
                    .matchesJsonSchemaString(INLINE_USER_SCHEMA);
        }

        @Test
        void invalidPayload_throwsAssertionError() {
            stubFor(get("/empty")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{}")));

            assertThrows(AssertionError.class, () ->
                    client.when().get("/empty")
                            .then()
                            .matchesJsonSchemaString(INLINE_USER_SCHEMA));
        }
    }

    @Nested
    class InputStreamSchema {

        @Test
        void validPayload_passes() {
            stubFor(get("/users/1")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(USER_JSON)));

            InputStream schema = JsonSchemaValidationTest.class
                    .getResourceAsStream("/schemas/user.json");

            client.when().get("/users/1")
                    .then()
                    .statusCode(200)
                    .matchesJsonSchema(schema);
        }
    }

    @Nested
    class SubDocumentValidation {

        @Test
        void nestedObject_validatedAgainstSchema() {
            String wrapped = """
                    {"status":"ok","data":{"id":1,"name":"Alice","email":"alice@example.com"}}
                    """.strip();
            stubFor(get("/wrapped")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(wrapped)));

            client.when().get("/wrapped")
                    .then()
                    .statusCode(200)
                    .body("data", matchesJsonSchema("schemas/user.json"));
        }

        @Test
        void nestedObject_withInlineSchema_fails_whenInvalid() {
            String wrapped = """
                    {"status":"ok","data":{"id":"wrong"}}
                    """.strip();
            stubFor(get("/wrapped-bad")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(wrapped)));

            assertThrows(AssertionError.class, () ->
                    client.when().get("/wrapped-bad")
                            .then()
                            .body("data", matchesJsonSchemaString(INLINE_USER_SCHEMA)));
        }
    }

    @Nested
    class ReusableSpec {

        @Test
        void spec_withJsonSchema_validatesBody() {
            ResponseSpec userSpec = ResponseSpec.builder()
                    .statusCode(200)
                    .jsonSchema("schemas/user.json")
                    .build();

            stubFor(get("/users/1")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(USER_JSON)));

            client.when().get("/users/1")
                    .then()
                    .spec(userSpec);
        }

        @Test
        void spec_withJsonSchema_failsWhenBodyInvalid() {
            ResponseSpec userSpec = ResponseSpec.builder()
                    .statusCode(200)
                    .jsonSchema("schemas/user.json")
                    .build();

            stubFor(get("/users/bad")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1}")));

            assertThrows(AssertionError.class, () ->
                    client.when().get("/users/bad")
                            .then()
                            .spec(userSpec));
        }
    }

    @Test
    void schemaValidation_chainedWithOtherAssertions() {
        stubFor(get("/users/1")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USER_JSON)));

        client.when().get("/users/1")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .matchesJsonSchema("schemas/user.json")
                .matchesJsonSchemaString(INLINE_USER_SCHEMA);
    }
}
