package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.log.LogCapture;
import io.smallrye.httpassured.log.RequestLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REST Assured compatibility tests -- logging DSL.
 *
 * <p>Covers request-side and response-side logging, including conditional
 * logging ({@code ifError}, {@code ifValidationFails}) and header blacklisting.
 * Each test installs a {@link LogCapture} to assert on logged output without
 * relying on any particular logging backend.</p>
 */
@WireMockTest
class LoggingTest {

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

    // Request logging

    @Nested
    class RequestLogging {

        @Test
        void shouldLogAllRequestDetails() {
            stubFor(get(urlEqualTo("/items"))
                    .willReturn(aResponse().withStatus(200).withBody("ok")));

            LogCapture capture = LogCapture.install();
            try {
                client.given()
                        .log().all()
                        .when().get("/items")
                        .then().statusCode(200);

                assertTrue(capture.hasInfo("GET"),
                        "Expected HTTP method 'GET' in request log");
                assertTrue(capture.hasInfo("/items"),
                        "Expected URI '/items' in request log");
            } finally {
                capture.uninstall();
            }
        }

        @Test
        void shouldLogRequestHeadersOnly() {
            stubFor(get(urlEqualTo("/headers"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"result\":\"secret-body\"}")));

            LogCapture capture = LogCapture.install();
            try {
                client.given()
                        .header("X-Trace-Id", "abc-123")
                        .log().headers()
                        .when().get("/headers")
                        .then().statusCode(200);

                assertTrue(capture.hasInfo("X-Trace-Id"),
                        "Expected header name in request log");
                assertTrue(capture.hasInfo("abc-123"),
                        "Expected header value in request log");
                assertTrue(capture.hasNone("secret-body"),
                        "Response body must not appear in request header log");
            } finally {
                capture.uninstall();
            }
        }
    }

    // Response logging

    @Nested
    class ResponseLogging {

        @Test
        void shouldLogAllResponseDetails() {
            stubFor(get(urlEqualTo("/data"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"Alice\"}")));

            LogCapture capture = LogCapture.install();
            try {
                client.when().get("/data")
                        .then()
                        .log().all()
                        .statusCode(200);

                assertTrue(capture.hasInfo("200"),
                        "Expected status code '200' in response log");
                assertTrue(capture.hasInfo("Content-Type"),
                        "Expected response header in log");
                assertTrue(capture.hasInfo("Alice"),
                        "Expected response body content in log");
            } finally {
                capture.uninstall();
            }
        }

        @Test
        void shouldLogResponseOnlyIfError() {
            // 200 response -- should NOT trigger ifError logging
            stubFor(get(urlEqualTo("/ok"))
                    .willReturn(aResponse().withStatus(200).withBody("all good")));

            LogCapture capture = LogCapture.install();
            try {
                client.when().get("/ok")
                        .then()
                        .log().ifError()
                        .statusCode(200);

                assertFalse(capture.hasError("Response"),
                        "ifError must NOT log when status is 200");

                // 500 response -- should trigger ifError logging
                capture.reset();

                stubFor(get(urlEqualTo("/error"))
                        .willReturn(aResponse().withStatus(500)
                                .withBody("internal failure")));

                client.when().get("/error")
                        .then()
                        .log().ifError();

                assertTrue(capture.hasError("Response"),
                        "ifError must log on 500");
                assertTrue(capture.hasError("500"),
                        "Expected status code '500' in error log");
            } finally {
                capture.uninstall();
            }
        }

        @Test
        void shouldLogResponseIfValidationFails() {
            stubFor(get(urlEqualTo("/check"))
                    .willReturn(aResponse().withStatus(200).withBody("fine")));

            LogCapture capture = LogCapture.install();
            try {
                AssertionError thrown = assertThrows(AssertionError.class, () ->
                        client.when().get("/check")
                                .then()
                                .log().ifValidationFails()
                                .statusCode(999)
                );

                assertNotNull(thrown, "AssertionError must be rethrown");
                assertTrue(capture.hasError("Response"),
                        "ifValidationFails must log on assertion failure");
            } finally {
                capture.uninstall();
            }
        }

        @Test
        void contentTypeMismatchShouldLogOnlyOnce() {
            stubFor(get(urlEqualTo("/ct"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("hello")));

            LogCapture capture = LogCapture.install();
            try {
                assertThrows(AssertionError.class, () ->
                        client.when().get("/ct")
                                .then()
                                .log().ifValidationFails()
                                .contentType("application/json"));

                long errorCount = capture.entries().stream()
                        .filter(e -> e.level() == org.jboss.logging.Logger.Level.ERROR)
                        .filter(e -> e.message() != null && e.message().contains("Response"))
                        .count();
                assertEquals(1, errorCount,
                        "contentType() failure should log the response exactly once, but logged " + errorCount + " times");
            } finally {
                capture.uninstall();
            }
        }

        @Test
        void shouldBlacklistSensitiveHeaders(WireMockRuntimeInfo wmInfo) {
            stubFor(get(urlEqualTo("/secure"))
                    .willReturn(aResponse().withStatus(200).withBody("ok")));

            LogCapture capture = LogCapture.install();
            try (HttpAssured secureClient = HttpAssured.builder()
                    .baseUri("http://localhost")
                    .port(wmInfo.getHttpPort())
                    .blacklistHeader("Authorization")
                    .build()) {

                secureClient.given()
                        .header("Authorization", "Bearer my-secret-token-xyz")
                        .log().all()
                        .when().get("/secure")
                        .then().statusCode(200);

                assertTrue(capture.hasInfo(RequestLogger.BLACKLISTED),
                        "Authorization header value must be masked as [ BLACKLISTED ]");
                assertTrue(capture.hasNone("my-secret-token-xyz"),
                        "The actual token must not appear in the log");
            } finally {
                capture.uninstall();
            }
        }
    }
}
