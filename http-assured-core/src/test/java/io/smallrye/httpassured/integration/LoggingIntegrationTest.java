package io.smallrye.httpassured.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.log.LogCapture;
import io.smallrye.httpassured.log.RequestLogger;
import io.smallrye.httpassured.log.ResponseLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the logging API.
 *
 * <p>Log output is captured via {@link LogCapture}, which installs a recording
 * {@link org.jboss.logging.Logger} directly into {@link RequestLogger} and
 * {@link ResponseLogger}. No stream redirection or SLF4J backend is needed.</p>
 */
@WireMockTest
class LoggingIntegrationTest {

    private HttpAssured client;
    private LogCapture capture;

    @BeforeEach
    void setup(WireMockRuntimeInfo wm) {
        capture = LogCapture.install();
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wm.getHttpPort())
                .build();
    }

    @AfterEach
    void teardown() {
        if (client != null) client.close();
        capture.uninstall();
    }

    @Test
    void request_logAll_emitsInfoRecord() {
        stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withStatus(200).withBody("ok")));

        client.given()
                .log().all()
                .when().get("/hello")
                .then().statusCode(200);

        assertTrue(capture.hasInfo("Request"), "Expected INFO log containing 'Request'");
        assertTrue(capture.hasInfo("GET"), "Expected INFO log containing HTTP method");
        assertTrue(capture.hasInfo("/hello"), "Expected INFO log containing URI path");
    }

    @Test
    void request_logHeaders_masksAuthorizationByDefault() {
        stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withStatus(200)));

        client.given()
                .header("Authorization", "Bearer secret-token")
                .header("X-Custom", "visible")
                .log().headers()
                .when().get("/hello")
                .then().statusCode(200);

        assertTrue(capture.hasInfo(RequestLogger.BLACKLISTED),
                "Authorization header value must be masked as [ BLACKLISTED ]");
        assertTrue(capture.hasNone("secret-token"),
                "The actual token must not appear in the log");
        assertTrue(capture.hasInfo("visible"),
                "Non-blacklisted header must appear as-is");
    }

    @Test
    void request_logHeaders_customBlacklistReplacesCookieWithXSecret(WireMockRuntimeInfo wm) {
        stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withStatus(200)));

        try (HttpAssured customClient = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wm.getHttpPort())
                .clearHeaderBlacklist()
                .blacklistHeader("X-Secret")
                .build()) {

            customClient.given()
                    .header("Authorization", "Bearer visible-now")
                    .header("X-Secret", "hidden-value")
                    .log().headers()
                    .when().get("/hello")
                    .then().statusCode(200);

            assertTrue(capture.hasInfo("visible-now"),
                    "Authorization must appear in plain text when removed from blacklist");
            assertTrue(capture.hasInfo(RequestLogger.BLACKLISTED),
                    "X-Secret must be masked");
            assertTrue(capture.hasNone("hidden-value"),
                    "The actual secret value must not appear in the log");
        }
    }

    @Test
    void request_logMethod_emitsOnlyMethod() {
        stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withStatus(200)));

        client.given()
                .log().method()
                .when().get("/hello")
                .then().statusCode(200);

        assertTrue(capture.hasInfo("GET"), "Expected method in log");
        assertTrue(capture.hasNone("/hello"), "URI must not appear when only method() logged");
    }

    @Test
    void request_logUri_emitsOnlyUri() {
        stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withStatus(200)));

        client.given()
                .log().uri()
                .when().get("/hello")
                .then().statusCode(200);

        assertTrue(capture.hasInfo("/hello"), "Expected URI in log");
        assertTrue(capture.hasNone("GET"), "Method must not appear when only uri() logged");
    }

    @Test
    void request_logParams_emitsQueryParams() {
        stubFor(get(urlPathEqualTo("/search")).willReturn(aResponse().withStatus(200).withBody("results")));

        client.given()
                .queryParam("q", "java")
                .log().params()
                .when().get("/search")
                .then().statusCode(200);

        assertTrue(capture.hasInfo("q"), "Expected query param key in log");
        assertTrue(capture.hasInfo("java"), "Expected query param value in log");
    }

    @Test
    void response_logAll_emitsInfoRecord() {
        stubFor(get(urlEqualTo("/data"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));

        client.when().get("/data")
                .then()
                .log().all()
                .statusCode(200);

        assertTrue(capture.hasInfo("Response"), "Expected 'Response' in log");
        assertTrue(capture.hasInfo("200"), "Expected status code in log");
        assertTrue(capture.hasInfo("ok"), "Expected body in log");
    }

    @Test
    void response_logStatus_emitsOnlyStatus() {
        stubFor(get(urlEqualTo("/status")).willReturn(aResponse().withStatus(204)));

        client.when().get("/status")
                .then()
                .log().status()
                .statusCode(204);

        assertTrue(capture.hasInfo("204"), "Expected status code in log");
        assertTrue(capture.hasNone("Body"), "Body section must not appear for status()-only log");
    }

    @Test
    void response_logIfError_logsOnFourXx() {
        stubFor(get(urlEqualTo("/missing"))
                .willReturn(aResponse().withStatus(404).withBody("not found")));

        client.when().get("/missing")
                .then()
                .log().ifError();

        assertTrue(capture.hasError("Response"), "ifError must log on 404");
        assertTrue(capture.hasError("404"), "Status code must appear in error log");
    }

    @Test
    void response_logIfError_doesNotLogOnSuccess() {
        stubFor(get(urlEqualTo("/ok")).willReturn(aResponse().withStatus(200).withBody("fine")));

        client.when().get("/ok")
                .then()
                .log().ifError()
                .statusCode(200);

        assertFalse(capture.hasError("Response"), "ifError must NOT log when status is 2xx");
    }

    @Test
    void response_logIfValidationFails_logsOnAssertionFailure() {
        stubFor(get(urlEqualTo("/bad")).willReturn(aResponse().withStatus(404).withBody("nope")));

        AssertionError thrown = assertThrows(AssertionError.class, () ->
                client.when().get("/bad")
                        .then()
                        .log().ifValidationFails()
                        .statusCode(200)
        );

        assertNotNull(thrown, "AssertionError must be rethrown");
        assertTrue(capture.hasError("Response"), "ifValidationFails must log on assertion failure");
    }

    @Test
    void response_logIfValidationFails_doesNotLogOnSuccess() {
        stubFor(get(urlEqualTo("/good")).willReturn(aResponse().withStatus(200).withBody("yep")));

        client.when().get("/good")
                .then()
                .log().ifValidationFails()
                .statusCode(200);

        assertFalse(capture.hasError("Response"), "ifValidationFails must NOT log when assertion passes");
    }

    @Test
    void response_masksBlacklistedResponseHeader(WireMockRuntimeInfo wm) {
        stubFor(get(urlEqualTo("/cookie"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Set-Cookie", "session=abc123; Path=/")));

        // Set-Cookie is not in the default blacklist — add it explicitly to test the mechanism
        try (HttpAssured masked = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wm.getHttpPort())
                .blacklistHeader("Set-Cookie")
                .build()) {

            masked.when().get("/cookie")
                    .then()
                    .log().headers()
                    .statusCode(200);

            assertTrue(capture.hasInfo(ResponseLogger.BLACKLISTED),
                    "Set-Cookie value must be masked");
            assertTrue(capture.hasNone("abc123"),
                    "Actual cookie value must not appear in the log");
        }
    }

    @Test
    void instanceLevel_logIfValidationFails_logsOnFailure(WireMockRuntimeInfo wm) {
        stubFor(get(urlEqualTo("/fail")).willReturn(aResponse().withStatus(404)));

        try (HttpAssured instanceClient = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wm.getHttpPort())
                .logIfValidationFails()
                .build()) {

            assertThrows(AssertionError.class, () ->
                    instanceClient.when().get("/fail")
                            .then()
                            .statusCode(200)
            );

            assertTrue(capture.hasError("Response"),
                    "Instance-level logIfValidationFails must log on failure");
        }
    }

    @Test
    void instanceLevel_logIfValidationFails_doesNotLogOnSuccess(WireMockRuntimeInfo wm) {
        stubFor(get(urlEqualTo("/pass")).willReturn(aResponse().withStatus(200)));

        try (HttpAssured instanceClient = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wm.getHttpPort())
                .logIfValidationFails()
                .build()) {

            instanceClient.when().get("/pass")
                    .then()
                    .statusCode(200);

            assertFalse(capture.hasError("Response"),
                    "Instance-level logIfValidationFails must NOT log when assertion passes");
        }
    }
}
