package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.options;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * REST Assured compatibility tests for OPTIONS requests.
 *
 * <p>Mirrors the patterns from REST Assured's {@code OptionsITest}.
 * OPTIONS requests are used to discover allowed methods and CORS capabilities.
 */
@WireMockTest
class OptionsTest {

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
    void shouldReturnAllowHeaderForOptions() {
        stubFor(options(urlEqualTo("/api"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Allow", "GET, POST, PUT, DELETE")));

        client.given()
                .when().options("/api")
                .then()
                .statusCode(200)
                .header("Allow", "GET, POST, PUT, DELETE");
    }

    @Test
    void shouldReturnCorsHeadersForOptions() {
        stubFor(options(urlEqualTo("/api"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Access-Control-Allow-Origin", "*")
                        .withHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")));

        client.given()
                .when().options("/api")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
                .headerExists("Access-Control-Allow-Origin")
                .headerExists("Access-Control-Allow-Methods");
    }

    @Test
    void shouldReturnStatusCodeForOptions() {
        stubFor(options(urlEqualTo("/api"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Allow", "GET, POST")));

        client.given()
                .when().options("/api")
                .then()
                .statusCode(204)
                .headerExists("Allow");
    }
}
