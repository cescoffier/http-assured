package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REST Assured compatibility tests — Headers, Cookies, Parameters.
 *
 * <p>Mirrors the patterns from the Baeldung article
 * <a href="https://www.baeldung.com/rest-assured-header-cookie-parameter">
 * REST Assured – Header, Cookie, and Parameter</a>.
 */
@WireMockTest
class HeadersParametersTest {

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
    class RequestHeaders {

        @Test
        void shouldSendRequestHeader() {
            stubFor(get(urlEqualTo("/header"))
                    .withHeader("X-Custom-Header", equalTo("test-value"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"value\":\"test-value\"}")));

            client.given()
                    .header("X-Custom-Header", "test-value")
                    .when().get("/header")
                    .then()
                    .statusCode(200)
                    .body("value", isEqualTo("test-value"));
        }

        @Test
        void shouldSendMultipleRequestHeaders() {
            stubFor(get(urlEqualTo("/multi-header"))
                    .withHeader("X-Header-1", equalTo("first"))
                    .withHeader("X-Header-2", equalTo("second"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"x1\":\"first\",\"x2\":\"second\"}")));

            client.given()
                    .header("X-Header-1", "first")
                    .header("X-Header-2", "second")
                    .when().get("/multi-header")
                    .then()
                    .statusCode(200)
                    .body("x1", isEqualTo("first"))
                    .body("x2", isEqualTo("second"));
        }
    }

    @Nested
    class ResponseHeaders {

        @Test
        void shouldAssertResponseHeaderExactValue() {
            stubFor(get(urlEqualTo("/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Response-Token", "abc-123")
                            .withBody("{\"ok\":true}")));

            client.given()
                    .when().get("/token")
                    .then()
                    .statusCode(200)
                    .header("X-Response-Token", "abc-123");
        }

        @Test
        void shouldAssertResponseHeaderExists() {
            stubFor(get(urlEqualTo("/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Response-Token", "abc-123")
                            .withBody("{\"ok\":true}")));

            client.given()
                    .when().get("/token")
                    .then()
                    .statusCode(200)
                    .headerExists("X-Response-Token");
        }

        @Test
        void shouldAssertResponseHeaderContainsSubstring() {
            stubFor(get(urlEqualTo("/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json; charset=UTF-8")
                            .withBody("{\"ok\":true}")));

            client.given()
                    .when().get("/token")
                    .then()
                    .statusCode(200)
                    .headerContains("Content-Type", "json");
        }
    }

    @Nested
    class OnFailMessageHeaders {

        @Test
        void headerAssertionShouldIncludeOnFailMessage() {
            stubFor(get(urlEqualTo("/onfail"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("X-Token", "wrong-value")
                            .withBody("ok")));

            AssertionError error = assertThrows(AssertionError.class, () ->
                    client.when().get("/onfail")
                            .then()
                            .onFailMessage("checking auth token")
                            .header("X-Token", "expected-value"));
            assertTrue(error.getMessage().contains("checking auth token"),
                    "Error should contain onFailMessage, was: " + error.getMessage());
        }

        @Test
        void headerContainsAssertionShouldIncludeOnFailMessage() {
            stubFor(get(urlEqualTo("/onfail-contains"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("ok")));

            AssertionError error = assertThrows(AssertionError.class, () ->
                    client.when().get("/onfail-contains")
                            .then()
                            .onFailMessage("checking content type")
                            .headerContains("Content-Type", "json"));
            assertTrue(error.getMessage().contains("checking content type"),
                    "Error should contain onFailMessage, was: " + error.getMessage());
        }

        @Test
        void headerExistsAssertionShouldIncludeOnFailMessage() {
            stubFor(get(urlEqualTo("/onfail-exists"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("ok")));

            AssertionError error = assertThrows(AssertionError.class, () ->
                    client.when().get("/onfail-exists")
                            .then()
                            .onFailMessage("expected auth header")
                            .headerExists("Authorization"));
            assertTrue(error.getMessage().contains("expected auth header"),
                    "Error should contain onFailMessage, was: " + error.getMessage());
        }
    }

    @Nested
    class Cookies {

        @Test
        void shouldSendCookieNameValue() {
            stubFor(get(urlEqualTo("/users/eugenp"))
                    .withHeader("Cookie", containing("session_id=1234"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"user\":\"eugenp\"}")));

            client.given()
                    .cookie("session_id", "1234")
                    .when().get("/users/eugenp")
                    .then()
                    .statusCode(200);
        }

        @Test
        void shouldSendCookieViaCookieBuilder() {
            Cookie myCookie = new Cookie.Builder("session_id", "1234")
                    .secure(true)
                    .comment("session id cookie")
                    .build();

            stubFor(get(urlEqualTo("/users/eugenp"))
                    .withHeader("Cookie", containing("session_id=1234"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"user\":\"eugenp\"}")));

            client.given()
                    .cookie(myCookie)
                    .when().get("/users/eugenp")
                    .then()
                    .statusCode(200);
        }

        @Test
        void shouldAssertResponseCookie() {
            stubFor(get(urlEqualTo("/session"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("Set-Cookie", "session_id=abc123; Path=/; HttpOnly")
                            .withBody("{\"ok\":true}")));

            client.given()
                    .when().get("/session")
                    .then()
                    .statusCode(200)
                    .cookie("session_id", "abc123");
        }

        @Test
        void shouldSendMultipleCookies() {
            stubFor(get(urlEqualTo("/multi-cookie"))
                    .withHeader("Cookie", containing("session_id=1234"))
                    .withHeader("Cookie", containing("token=xyz"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"ok\":true}")));

            client.given()
                    .cookie("session_id", "1234")
                    .cookie("token", "xyz")
                    .when().get("/multi-cookie")
                    .then()
                    .statusCode(200);
        }

        @Test
        void shouldAssertCookieExists() {
            stubFor(get(urlEqualTo("/session"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("Set-Cookie", "session_id=abc123; Path=/")
                            .withBody("{\"ok\":true}")));

            client.given()
                    .when().get("/session")
                    .then()
                    .statusCode(200)
                    .cookieExists("session_id");
        }

        @Test
        void shouldAssertMultipleResponseCookies() {
            stubFor(get(urlEqualTo("/multi-set-cookie"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("Set-Cookie", "session_id=abc123; Path=/")
                            .withHeader("Set-Cookie", "token=xyz789; HttpOnly")
                            .withBody("{\"ok\":true}")));

            client.given()
                    .when().get("/multi-set-cookie")
                    .then()
                    .statusCode(200)
                    .cookie("session_id", "abc123")
                    .cookie("token", "xyz789");
        }
    }

    @Nested
    class QueryParameters {

        @Test
        void shouldSendSingleQueryParam() {
            stubFor(get(urlPathEqualTo("/echo-params"))
                    .withQueryParam("page", equalTo("2"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"page\":\"2\"}")));

            client.given()
                    .queryParam("page", "2")
                    .when().get("/echo-params")
                    .then()
                    .statusCode(200)
                    .body("page", isEqualTo("2"));
        }

        @Test
        void shouldSendMultipleQueryParams() {
            stubFor(get(urlPathEqualTo("/echo-params"))
                    .withQueryParam("page", equalTo("2"))
                    .withQueryParam("size", equalTo("20"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"page\":\"2\",\"size\":\"20\"}")));

            client.given()
                    .queryParam("page", "2")
                    .queryParam("size", "20")
                    .when().get("/echo-params")
                    .then()
                    .statusCode(200)
                    .body("page", isEqualTo("2"))
                    .body("size", isEqualTo("20"));
        }
    }

    @Nested
    class PathParameters {

        @Test
        void shouldUsePositionalPathParam() {
            stubFor(get(urlEqualTo("/users/5"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":5,\"name\":\"User-5\"}")));

            client.given()
                    .when().get("/users/{id}", 5)
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(5))
                    .body("name", isEqualTo("User-5"));
        }

        @Test
        void shouldUseNamedPathParam() {
            stubFor(get(urlEqualTo("/users/5"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":5,\"name\":\"User-5\"}")));

            client.given()
                    .pathParam("id", "5")
                    .when().get("/users/{id}")
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(5))
                    .body("name", isEqualTo("User-5"));
        }

        @Test
        void shouldUseMultipleNamedPathParams() {
            stubFor(get(urlEqualTo("/repos/baeldung/tutorials"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"owner\":\"baeldung\",\"repo\":\"tutorials\"}")));

            client.given()
                    .pathParam("owner", "baeldung")
                    .pathParam("repo", "tutorials")
                    .when().get("/repos/{owner}/{repo}")
                    .then()
                    .statusCode(200)
                    .body("owner", isEqualTo("baeldung"))
                    .body("repo", isEqualTo("tutorials"));
        }
    }
}
