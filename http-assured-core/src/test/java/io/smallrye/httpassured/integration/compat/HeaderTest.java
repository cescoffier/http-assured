package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * REST Assured compatibility tests — advanced header assertions.
 *
 * <p>Mirrors patterns from REST Assured's {@code HeaderITest}, focusing on
 * scenarios not already covered by {@link HeadersParametersTest}.
 */
@WireMockTest
class HeaderTest {

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
    void shouldAssertHeaderCaseInsensitive() {
        stubFor(get(urlEqualTo("/case"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));

        // Server sends "Content-Type" but we assert with lowercase "content-type"
        client.when().get("/case")
                .then()
                .statusCode(200)
                .header("content-type", "application/json");
    }

    @Test
    void shouldAssertMultipleResponseHeaders() {
        stubFor(get(urlEqualTo("/multi"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Request-Id", "req-001")
                        .withHeader("X-Correlation-Id", "corr-002")
                        .withBody("{\"ok\":true}")));

        client.when().get("/multi")
                .then()
                .statusCode(200)
                .header("X-Request-Id", "req-001")
                .header("X-Correlation-Id", "corr-002")
                .header("Content-Type", "application/json");
    }

    @Test
    void shouldVerifyHeaderContainsSubstringPattern() {
        stubFor(get(urlEqualTo("/charset"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                        .withBody("{\"ok\":true}")));

        client.when().get("/charset")
                .then()
                .statusCode(200)
                .headerContains("Content-Type", "charset=UTF-8");
    }

    @Test
    void shouldReadHeaderFromResponseObject() {
        stubFor(get(urlEqualTo("/custom"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Custom", "hello-world")
                        .withBody("{\"ok\":true}")));

        Response response = client.when().get("/custom");
        assertEquals("hello-world", response.getHeader("X-Custom"));
    }

    @Test
    void shouldVerifyHeaderExistsAndValue() {
        stubFor(get(urlEqualTo("/request-id"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Request-Id", "abc123")
                        .withBody("{\"ok\":true}")));

        client.when().get("/request-id")
                .then()
                .statusCode(200)
                .headerExists("X-Request-Id")
                .header("X-Request-Id", "abc123");
    }
}
