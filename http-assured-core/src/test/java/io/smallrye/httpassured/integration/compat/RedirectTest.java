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
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * REST Assured compatibility tests — redirect control.
 *
 * <p>Mirrors patterns from REST Assured's {@code RedirectITest}, covering
 * redirect following, disabling, chain traversal, and max redirect limits.
 *
 * <p>Differences from REST Assured:
 * <ul>
 *   <li>http-assured supports redirect config per-request only; REST Assured
 *       also supports global config via {@code RestAssured.config().redirect(...)}</li>
 *   <li>{@code allowCircular(true)} is not supported</li>
 * </ul>
 */
@WireMockTest
class RedirectTest {

    private HttpAssured client;

    @BeforeEach
    void setupClient(WireMockRuntimeInfo wmInfo) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .build();

        // Single redirect: /redirect -> /target
        stubFor(get(urlEqualTo("/redirect"))
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader("Location", "/target")));

        stubFor(get(urlEqualTo("/target"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));

        // 301 redirect: /moved -> /new-location
        stubFor(get(urlEqualTo("/moved"))
                .willReturn(aResponse()
                        .withStatus(301)
                        .withHeader("Location", "/new-location")));

        stubFor(get(urlEqualTo("/new-location"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"moved\":true}")));

        // Redirect chain: /chain1 -> /chain2 -> /chain3 (200)
        stubFor(get(urlEqualTo("/chain1"))
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader("Location", "/chain2")));

        stubFor(get(urlEqualTo("/chain2"))
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader("Location", "/chain3")));

        stubFor(get(urlEqualTo("/chain3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"chain\":\"complete\"}")));
    }

    @AfterEach
    void closeClient() {
        if (client != null) client.close();
    }

    @Test
    void shouldFollowRedirectsByDefault() {
        client.when().get("/redirect")
                .then()
                .statusCode(200)
                .body("status", isEqualTo("ok"));
    }

    @Test
    void shouldNotFollowRedirectsWhenDisabled() {
        client.given()
                .redirects().follow(false)
                .when().get("/redirect")
                .then()
                .statusCode(302);
    }

    @Test
    void shouldFollowRedirectsWhenExplicitlyEnabled() {
        client.given()
                .redirects().follow(true)
                .when().get("/redirect")
                .then()
                .statusCode(200)
                .body("status", isEqualTo("ok"));
    }

    @Test
    void shouldHandleRedirectChain() {
        client.when().get("/chain1")
                .then()
                .statusCode(200)
                .body("chain", isEqualTo("complete"));
    }

    @Test
    void shouldRespectMaxRedirects() {
        // With max(1), the client follows one redirect: /chain1 -> /chain2
        // Then stops at /chain2's 302 response (the second redirect to /chain3 is not followed)
        Response response = client.given()
                .redirects().max(1)
                .when().get("/chain1");

        // After following 1 redirect, we arrive at /chain2 which is another 302
        assertEquals(302, response.statusCode());
    }

    @Test
    void shouldGet301WithLocationHeader() {
        Response response = client.given()
                .redirects().follow(false)
                .when().get("/moved");

        assertEquals(301, response.statusCode());
        String location = response.getHeader("Location");
        assertNotNull(location, "Location header should be present on 301 response");
        assertEquals("/new-location", location);
    }
}
