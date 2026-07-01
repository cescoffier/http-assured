package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REST Assured compatibility tests — response extraction.
 *
 * <p>Mirrors the patterns from the Baeldung article
 * <a href="https://www.baeldung.com/rest-assured-response">
 * Getting and Validating a Response with REST Assured</a>.
 *
 * <p><b>Header access — REST Assured vs http-assured:</b>
 * <ul>
 *   <li>REST Assured: {@code response.getHeader("X-App-Version")} returns {@code String} directly
 *       (or {@code null} if absent).</li>
 *   <li>http-assured idiomatic: {@code response.headers().getValue("X-App-Version")} returns
 *       {@code Optional<String>}.</li>
 *   <li>http-assured convenience (REST Assured parity): {@code response.getHeader("X-App-Version")}
 *       returns {@code String} directly ({@code null} if absent).</li>
 * </ul>
 */
@WireMockTest
class ResponseExtractionTest {

    private HttpAssured client;

    @BeforeEach
    void setupClient(WireMockRuntimeInfo wmInfo) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .build();
    }

    @AfterEach
    void closeClient() {
        if (client != null) client.close();
    }

    @Nested
    class BodyExtraction {

        @Test
        void shouldGetBodyAsString() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"Bob\",\"email\":\"bob@example.com\"}")));

            Response response = client.given().when().get("/users/1");
            String body = response.bodyAsString();
            assertNotNull(body);
            assertTrue(body.contains("Bob"));
        }

        @Test
        void shouldAccessBodyAsBytes() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"Bob\",\"email\":\"bob@example.com\"}")));

            Response response = client.given().when().get("/users/1");
            byte[] bytes = response.bodyAsBytes();
            assertNotNull(bytes);
            assertTrue(bytes.length > 0);
        }

        @Test
        void shouldDeserializeBodyToPojoViaResponse() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"Bob\",\"email\":\"bob@example.com\"}")));

            User user = client.given().when().get("/users/1").bodyAs(User.class);
            assertEquals("Bob", user.name);
            assertEquals("bob@example.com", user.email);
        }

        @Test
        void shouldDeserializeBodyToPojoViaThen() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"Bob\",\"email\":\"bob@example.com\"}")));

            User user = client.given()
                    .when().get("/users/1")
                    .then()
                    .statusCode(200)
                    .extractAs(User.class);
            assertEquals("Bob", user.name);
            assertEquals("bob@example.com", user.email);
        }

        @Test
        void shouldChainAssertionThenExtract() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"Bob\",\"email\":\"bob@example.com\"}")));

            User user = client.given()
                    .when().get("/users/1")
                    .then()
                    .statusCode(200)
                    .body("name", isNotNull())
                    .extractAs(User.class);
            assertEquals(1, user.id);
            assertEquals("Bob", user.name);
        }
    }

    @Nested
    class JsonPathExtraction {

        @Test
        void shouldExtractJsonPathStringValue() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"Bob\",\"email\":\"bob@example.com\"}")));

            String name = client.given()
                    .when().get("/users/1")
                    .then()
                    .extract("name");
            assertEquals("Bob", name);
        }

        @Test
        void shouldExtractJsonPathNumericValue() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"Bob\",\"email\":\"bob@example.com\"}")));

            int id = client.given()
                    .when().get("/users/1")
                    .then()
                    .extract("id");
            assertEquals(1, id);
        }
    }

    @Nested
    class StatusAndHeaders {

        @Test
        void shouldGetStatusCode() {
            stubFor(get(urlEqualTo("/health"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-App-Version", "1.0")
                            .withBody("{\"status\":\"up\"}")));

            Response response = client.given().when().get("/health");
            assertEquals(200, response.statusCode());
        }

        @Test
        void shouldGetResponseHeaderValue() {
            stubFor(get(urlEqualTo("/health"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-App-Version", "1.0")
                            .withBody("{\"status\":\"up\"}")));

            Response response = client.given().when().get("/health");
            // idiomatic Optional path
            assertTrue(response.headers().getValue("X-App-Version").isPresent());
            assertEquals("1.0", response.headers().getValue("X-App-Version").get());
            // REST Assured parity convenience method
            assertEquals("1.0", response.getHeader("X-App-Version"));
        }

        @Test
        void shouldGetContentTypeHeader() {
            stubFor(get(urlEqualTo("/health"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-App-Version", "1.0")
                            .withBody("{\"status\":\"up\"}")));

            Response response = client.given().when().get("/health");
            assertTrue(response.headers().getValue("Content-Type")
                    .map(v -> v.contains("application/json"))
                    .orElse(false));
        }
    }

    @Nested
    class GetHeaderConvenience {

        @Test
        void shouldGetHeaderByName() {
            stubFor(get(urlEqualTo("/ping"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("X-Request-Id", "abc-123")
                            .withBody("")));

            Response response = client.given().when().get("/ping");
            assertEquals("abc-123", response.getHeader("X-Request-Id"));
        }

        @Test
        void shouldReturnNullForAbsentHeader() {
            stubFor(get(urlEqualTo("/ping"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("")));

            Response response = client.given().when().get("/ping");
            assertNull(response.getHeader("X-Request-Id"));
        }

        @Test
        void shouldGetHeaderCaseInsensitively() {
            stubFor(get(urlEqualTo("/ping"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("X-Request-Id", "abc-123")
                            .withBody("")));

            Response response = client.given().when().get("/ping");
            assertEquals("abc-123", response.getHeader("x-request-id"));
        }
    }

    public static class User {
        public int id;
        public String name;
        public String email;

        public User() {}
    }
}
