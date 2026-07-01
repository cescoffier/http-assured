package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.greaterThan;
import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;

/**
 * REST Assured compatibility tests — given/when/then DSL variations.
 *
 * <p>Mirrors patterns from REST Assured's {@code GivenWhenThenITest}.
 * Focuses on chaining, body-level assertions, and the {@code when()} shortcut.
 * JSON only — no XPath.
 */
@WireMockTest
class GivenWhenThenTest {

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

    // Chaining and DSL syntax

    @Test
    void shouldChainMultipleBodyAssertions() {
        stubFor(get(urlEqualTo("/products/42"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":42,\"name\":\"Widget\",\"price\":9.99,\"stock\":150}")));

        client.given()
                .when().get("/products/{id}", 42)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(42))
                .body("name", isEqualTo("Widget"))
                .body("price", isEqualTo(9.99))
                .body("stock", greaterThan(0));
    }

    @Test
    void shouldUseGivenWhenThenFullSyntax() {
        stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("widgets"))
                .withHeader("X-Client", equalTo("test-agent"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"name\":\"Widget A\"},{\"name\":\"Widget B\"}],\"total\":2}")));

        client.given()
                .header("X-Client", "test-agent")
                .queryParam("q", "widgets")
                .when().get("/search")
                .then()
                .statusCode(200)
                .body("total", isEqualTo(2))
                .body("results", hasSize(2))
                .body("results[0].name", isEqualTo("Widget A"));
    }

    @Test
    void shouldUseWhenWithoutGiven() {
        stubFor(get(urlEqualTo("/status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"service\":\"api\",\"healthy\":true}")));

        client.when().get("/status")
                .then()
                .statusCode(200)
                .body("service", isEqualTo("api"))
                .body("healthy", isNotNull());
    }

    // Full-body assertions

    @Test
    void shouldAssertBodyEqualsFullString() {
        String expectedBody = "{\"ok\":true}";
        stubFor(get(urlEqualTo("/ping"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expectedBody)));

        client.when().get("/ping")
                .then()
                .statusCode(200)
                .bodyEquals(expectedBody);
    }

    @Test
    void shouldAssertBodyContainsSubstring() {
        stubFor(get(urlEqualTo("/info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"version\":\"2.4.1\",\"build\":\"20240115-abc\"}")));

        client.when().get("/info")
                .then()
                .statusCode(200)
                .bodyContains("2.4.1")
                .bodyContains("20240115");
    }
}
