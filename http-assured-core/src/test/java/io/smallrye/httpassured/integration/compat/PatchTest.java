package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;

/**
 * REST Assured compatibility tests for PATCH operations.
 *
 * <p>Covers patterns from REST Assured's {@code PatchITest}, exercising
 * partial updates with JSON, string, and object bodies.
 */
@WireMockTest
class PatchTest {

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

    // PATCH with JSON body

    @Test
    void shouldPatchWithJsonBody() {
        stubFor(patch(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Alice\",\"age\":31}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"age\":31}")
                .when().patch("/users/{id}", 1)
                .then()
                .statusCode(200)
                .body("age", isEqualTo(31))
                .body("name", isEqualTo("Alice"));
    }

    // PATCH with raw string body

    @Test
    void shouldPatchWithStringBody() {
        stubFor(patch(urlEqualTo("/users/2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":2,\"name\":\"Bob Updated\",\"age\":30}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Bob Updated\"}")
                .when().patch("/users/{id}", 2)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(2))
                .body("name", isEqualTo("Bob Updated"));
    }

    // PATCH with object body (Map auto-serialized)

    @Test
    void shouldPatchWithObjectBody() {
        stubFor(patch(urlEqualTo("/users/3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":3,\"name\":\"Charlie\",\"age\":25,\"email\":\"charlie@example.com\"}")));

        Map<String, Object> patchData = Map.of("age", 25, "email", "charlie@example.com");

        client.given()
                .body(patchData)
                .when().patch("/users/{id}", 3)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(3))
                .body("age", isEqualTo(25))
                .body("email", isEqualTo("charlie@example.com"));
    }

    // PATCH only changes specified fields

    @Test
    void shouldPatchAndReturnPartialUpdate() {
        stubFor(patch(urlEqualTo("/products/7"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":7,\"name\":\"Original Name\",\"price\":49.99,\"stock\":100}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"price\":49.99}")
                .when().patch("/products/{id}", 7)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(7))
                .body("name", isEqualTo("Original Name"))
                .body("price", isEqualTo(49.99))
                .body("stock", isEqualTo(100));
    }
}
