package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.containsString;
import static io.smallrye.httpassured.assertion.Assertions.greaterThan;
import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * REST Assured compatibility tests - multi-path body assertions.
 *
 * <p>Mirrors REST Assured's {@code body("path1", matcher1, "path2", matcher2)}
 * varargs overload, using http-assured's {@code BodyAssertion} instead of
 * Hamcrest matchers.
 */
@WireMockTest
class MultiPathBodyAssertionTest {

    private static final String STORE_JSON = """
            {
              "store": {
                "name": "BookStore",
                "owner": "John Doe",
                "books": [
                  {"title": "Clean Code"},
                  {"title": "Refactoring"}
                ]
              }
            }
            """;

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

    @Test
    void shouldAssertMultiplePaths() {
        stubFor(get(urlEqualTo("/store"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STORE_JSON)));

        client.given()
                .when().get("/store")
                .then()
                .body("store.name", isEqualTo("BookStore"),
                        "store.owner", containsString("John"));
    }

    @Test
    void shouldAssertThreePaths() {
        stubFor(get(urlEqualTo("/store"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STORE_JSON)));

        client.given()
                .when().get("/store")
                .then()
                .body("store.name", isEqualTo("BookStore"),
                        "store.books", hasSize(2),
                        "store.owner", containsString("Doe"));
    }

    @Test
    void shouldFailOnFirstMismatch() {
        stubFor(get(urlEqualTo("/store"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STORE_JSON)));

        assertThrows(AssertionError.class, () ->
                client.given()
                        .when().get("/store")
                        .then()
                        .body("store.name", isEqualTo("WrongName"),
                                "store.owner", containsString("John")));
    }

    @Test
    void shouldFailOnSecondMismatch() {
        stubFor(get(urlEqualTo("/store"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STORE_JSON)));

        assertThrows(AssertionError.class, () ->
                client.given()
                        .when().get("/store")
                        .then()
                        .body("store.name", isEqualTo("BookStore"),
                                "store.owner", containsString("WRONG")));
    }

    @Test
    void shouldRejectOddArgCount() {
        stubFor(get(urlEqualTo("/store"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STORE_JSON)));

        assertThrows(IllegalArgumentException.class, () ->
                client.given()
                        .when().get("/store")
                        .then()
                        .body("store.name", isEqualTo("BookStore"),
                                "store.owner"));
    }

    @Test
    void shouldWorkWithSinglePair() {
        stubFor(get(urlEqualTo("/store"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STORE_JSON)));

        // This should resolve to the original body(String, BodyAssertion) overload
        client.given()
                .when().get("/store")
                .then()
                .body("store.name", isEqualTo("BookStore"));
    }

    @Test
    void shouldChainWithOtherAssertions() {
        stubFor(get(urlEqualTo("/store"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STORE_JSON)));

        client.given()
                .when().get("/store")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("store.name", isEqualTo("BookStore"),
                        "store.owner", isNotNull(),
                        "store.books", hasSize(2))
                .body("store.books[0].title", isEqualTo("Clean Code"));
    }
}
