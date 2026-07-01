package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2 compat tests for ValidatableResponse features:
 * statusLine, rootPath, and/assertThat, onFailMessage, headers variadic.
 */
@WireMockTest
class P2ValidatableResponseTest {

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
    class StatusLine {

        @Test
        void shouldAssertStatusLineContainsCodeAndMessage() {
            stubFor(get(urlEqualTo("/ok"))
                    .willReturn(aResponse().withStatus(200)));

            client.when().get("/ok")
                    .then()
                    .statusLine("200 OK");
        }

        @Test
        void shouldFailWhenStatusLineDoesNotMatch() {
            stubFor(get(urlEqualTo("/ok"))
                    .willReturn(aResponse().withStatus(200)));

            assertThrows(AssertionError.class, () ->
                    client.when().get("/ok")
                            .then()
                            .statusLine("404 Not Found"));
        }
    }

    @Nested
    class RootPath {

        @Test
        void shouldUseRootPathForBodyAssertions() {
            stubFor(get(urlEqualTo("/nested"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"name\":\"BookShop\",\"city\":\"Paris\"}}")));

            client.when().get("/nested")
                    .then()
                    .statusCode(200)
                    .rootPath("store")
                    .body("name", isEqualTo("BookShop"))
                    .body("city", isEqualTo("Paris"));
        }

        @Test
        void shouldAppendToRootPath() {
            stubFor(get(urlEqualTo("/deep"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"a\":{\"b\":{\"c\":\"deep\"}}}")));

            client.when().get("/deep")
                    .then()
                    .statusCode(200)
                    .rootPath("a")
                    .appendRootPath("b")
                    .body("c", isEqualTo("deep"));
        }

        @Test
        void shouldDetachRootPath() {
            stubFor(get(urlEqualTo("/detach"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"name\":\"BookShop\"},\"version\":1}")));

            client.when().get("/detach")
                    .then()
                    .statusCode(200)
                    .rootPath("store")
                    .body("name", isEqualTo("BookShop"))
                    .detachRootPath()
                    .body("version", isEqualTo(1));
        }

        @Test
        void shouldBypassRootPathWithDollarPrefix() {
            stubFor(get(urlEqualTo("/dollar"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"name\":\"BookShop\"},\"version\":2}")));

            client.when().get("/dollar")
                    .then()
                    .statusCode(200)
                    .rootPath("store")
                    .body("name", isEqualTo("BookShop"))
                    .body("$.version", isEqualTo(2));
        }
    }

    @Nested
    class ChainingMethods {

        @Test
        void shouldSupportAndAndAssertThat() {
            stubFor(get(urlEqualTo("/chain"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Custom", "value")
                            .withBody("{\"id\":1}")));

            client.when().get("/chain")
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .and()
                    .body("id", isEqualTo(1));
        }
    }

    @Nested
    class OnFailMessage {

        @Test
        void shouldPrependCustomMessageOnFailure() {
            stubFor(get(urlEqualTo("/fail"))
                    .willReturn(aResponse()
                            .withStatus(404)));

            AssertionError error = assertThrows(AssertionError.class, () ->
                    client.when().get("/fail")
                            .then()
                            .onFailMessage("User lookup failed")
                            .statusCode(200));

            assertTrue(error.getMessage().contains("User lookup failed"),
                    "Error message should contain custom message, was: " + error.getMessage());
        }
    }

    @Nested
    class HeadersVariadic {

        @Test
        void shouldAssertMultipleHeaders() {
            stubFor(get(urlEqualTo("/headers"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Request-Id", "abc-123")
                            .withHeader("X-Powered-By", "http-assured")));

            client.when().get("/headers")
                    .then()
                    .statusCode(200)
                    .headers("Content-Type", "application/json",
                            "X-Request-Id", "abc-123",
                            "X-Powered-By", "http-assured");
        }

        @Test
        void shouldRejectOddNumberOfAdditionalArguments() {
            stubFor(get(urlEqualTo("/headers-odd"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "text/plain")));

            assertThrows(IllegalArgumentException.class, () ->
                    client.when().get("/headers-odd")
                            .then()
                            .headers("Content-Type", "text/plain", "orphan"));
        }
    }
}
