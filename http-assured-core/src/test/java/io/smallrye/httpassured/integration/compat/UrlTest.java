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
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;

/**
 * REST Assured compatibility tests -- URL construction.
 *
 * <p>Verifies that the builder's {@code baseUri}, {@code port}, and
 * {@code basePath} options compose correctly when combined with the
 * request path passed to {@code get()}/{@code post()}/etc.
 */
@WireMockTest
class UrlTest {

    private int port;

    @BeforeEach
    void capturePort(WireMockRuntimeInfo wmInfo) {
        port = wmInfo.getHttpPort();
    }

    @AfterEach
    void closeClient() {
        // Each test creates its own client with a specific builder config,
        // so cleanup is handled per-test.
    }

    @Test
    void shouldConstructUrlFromBaseUriAndPath() {
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"count\":3}")));

        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(port)
                .build();
        try {
            client.given()
                    .when().get("/users")
                    .then()
                    .statusCode(200)
                    .body("count", isEqualTo(3));
        } finally {
            client.close();
        }
    }

    @Test
    void shouldConstructUrlWithBasePath() {
        stubFor(get(urlEqualTo("/api/v1/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"count\":5}")));

        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(port)
                .basePath("/api/v1")
                .build();
        try {
            client.given()
                    .when().get("/users")
                    .then()
                    .statusCode(200)
                    .body("count", isEqualTo(5));
        } finally {
            client.close();
        }
    }

    @Test
    void shouldHandleTrailingSlashInBaseUri() {
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"count\":2}")));

        // Trailing slash in baseUri + path without leading slash should
        // produce a clean URL with no double-slash.
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost/")
                .port(port)
                .build();
        try {
            client.given()
                    .when().get("users")
                    .then()
                    .statusCode(200)
                    .body("count", isEqualTo(2));
        } finally {
            client.close();
        }
    }

    @Test
    void shouldOverridePortAlreadyInBaseUri() {
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));

        // baseUri contains port 9999, but .port() overrides it to the
        // actual WireMock port so the request reaches the stub.
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:9999")
                .port(port)
                .build();
        try {
            client.given()
                    .when().get("/test")
                    .then()
                    .statusCode(200)
                    .body("ok", isEqualTo(true));
        } finally {
            client.close();
        }
    }
}
