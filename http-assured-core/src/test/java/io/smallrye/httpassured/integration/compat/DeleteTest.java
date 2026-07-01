package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;

/**
 * REST Assured compatibility tests for DELETE operations.
 *
 * <p>Covers patterns from REST Assured's {@code DeleteITest}, exercising
 * 204 No Content responses, JSON confirmation bodies, and path-param
 * substitution on DELETE endpoints.
 */
@WireMockTest
class DeleteTest {

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

    // DELETE returning 204 No Content

    @Test
    void shouldDeleteAndReturn204() {
        stubFor(delete(urlEqualTo("/orders/42"))
                .willReturn(aResponse()
                        .withStatus(204)));

        client.given()
                .when().delete("/orders/{id}", 42)
                .then()
                .statusCode(204);
    }

    // DELETE returning JSON confirmation body

    @Test
    void shouldDeleteWithJsonResponseBody() {
        stubFor(delete(urlEqualTo("/orders/42"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"deleted\",\"id\":42}")));

        client.given()
                .when().delete("/orders/{id}", 42)
                .then()
                .statusCode(200)
                .body("status", isEqualTo("deleted"))
                .body("id", isEqualTo(42));
    }

    // DELETE with positional path parameter

    @Test
    void shouldDeleteWithPathParam() {
        stubFor(delete(urlEqualTo("/users/99"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"User 99 removed\"}")));

        client.given()
                .when().delete("/users/{id}", 99)
                .then()
                .statusCode(200)
                .body("message", isEqualTo("User 99 removed"));
    }
}
