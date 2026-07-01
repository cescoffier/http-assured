package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;

/**
 * REST Assured compatibility tests — JSON GET operations.
 *
 * <p>Mirrors the patterns from REST Assured's {@code JSONGetITest},
 * covering JSON path traversal, array indexing, query/path parameters,
 * and multi-path assertions.
 *
 * <p>Complements {@link RestAssuredTutorialTest} which covers basic
 * GET/POST/PUT/DELETE/PATCH and simple query params. This test class
 * focuses on nested paths, array element access, and content-type
 * assertions.
 */
@WireMockTest
class JsonGetTest {

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
    class SimpleJsonResponses {

        @Test
        void shouldGetSimpleJsonObject() {
            stubFor(get(urlEqualTo("/product"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"test\"}")));

            client.when().get("/product")
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(1))
                    .body("name", isEqualTo("test"));
        }

        @Test
        void shouldAssertStatusCodeAndContentType() {
            stubFor(get(urlEqualTo("/health"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"status\":\"UP\"}")));

            client.when().get("/health")
                    .then()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("status", isEqualTo("UP"));
        }

        @Test
        void shouldAssertMultipleBodyPaths() {
            stubFor(get(urlEqualTo("/item"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":42,\"name\":\"Widget\",\"price\":9.99,\"inStock\":true}")));

            client.when().get("/item")
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(42))
                    .body("name", isEqualTo("Widget"))
                    .body("price", isEqualTo(9.99))
                    .body("inStock", isEqualTo(true));
        }
    }

    @Nested
    class NestedAndArrayPaths {

        @Test
        void shouldGetNestedJsonPath() {
            stubFor(get(urlEqualTo("/catalog"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"book\":{\"title\":\"Foo\",\"author\":\"Bar\"}}}")));

            client.when().get("/catalog")
                    .then()
                    .statusCode(200)
                    .body("store.book.title", isEqualTo("Foo"))
                    .body("store.book.author", isEqualTo("Bar"));
        }

        @Test
        void shouldGetJsonArray() {
            stubFor(get(urlEqualTo("/items"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\":1},{\"id\":2}]")));

            client.when().get("/items")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(2));
        }

        @Test
        void shouldGetArrayElementByIndex() {
            stubFor(get(urlEqualTo("/items"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\":1,\"name\":\"First\"},{\"id\":2,\"name\":\"Second\"}]")));

            client.when().get("/items")
                    .then()
                    .statusCode(200)
                    .body("[0].id", isEqualTo(1))
                    .body("[0].name", isEqualTo("First"))
                    .body("[1].id", isEqualTo(2));
        }
    }

    @Nested
    class ParameterizedRequests {

        @Test
        void shouldGetWithQueryParamsAndJsonResponse() {
            stubFor(get(urlPathEqualTo("/search"))
                    .withQueryParam("category", equalTo("electronics"))
                    .withQueryParam("sort", equalTo("price"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"category\":\"electronics\",\"sort\":\"price\",\"count\":5}")));

            client.given()
                    .queryParam("category", "electronics")
                    .queryParam("sort", "price")
                    .when().get("/search")
                    .then()
                    .statusCode(200)
                    .body("category", isEqualTo("electronics"))
                    .body("sort", isEqualTo("price"))
                    .body("count", isEqualTo(5));
        }

        @Test
        void shouldGetWithPathParamAndJsonResponse() {
            stubFor(get(urlEqualTo("/products/77"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":77,\"name\":\"Gadget\",\"category\":\"electronics\"}")));

            client.when().get("/products/{id}", 77)
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(77))
                    .body("name", isEqualTo("Gadget"))
                    .body("category", isEqualTo("electronics"));
        }
    }
}
