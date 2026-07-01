package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REST Assured compatibility tests for HEAD requests.
 *
 * <p>Mirrors the patterns from REST Assured's {@code HeadITest}.
 * HEAD requests return headers and status but no body.
 */
@WireMockTest
class HeadTest {

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
    void shouldReturnHeadersForHeadRequest() {
        stubFor(head(urlEqualTo("/resources"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")));

        client.given()
                .when().head("/resources")
                .then()
                .statusCode(200)
                .header("Content-Type", "application/json");
    }

    @Test
    void shouldReturnEmptyBodyForHeadRequest() {
        stubFor(head(urlEqualTo("/resources"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")));

        Response response = client.when().head("/resources");
        assertEquals(200, response.statusCode());
        assertNotNull(response.bodyAsString());
        assertTrue(response.bodyAsString().isEmpty(),
                "HEAD response body should be empty");
    }

    @Test
    void shouldReturnCustomHeadersForHeadRequest() {
        stubFor(head(urlEqualTo("/resources"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("X-Custom-Id", "abc-123")
                        .withHeader("X-Request-Count", "42")));

        client.given()
                .when().head("/resources")
                .then()
                .statusCode(200)
                .header("X-Custom-Id", "abc-123")
                .header("X-Request-Count", "42")
                .headerExists("X-Custom-Id")
                .headerExists("X-Request-Count");
    }
}
