package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;

/**
 * P2 compat tests for request-side features:
 * urlEncodingEnabled, no-value params, request(Method), conditional logging.
 */
@WireMockTest
class P2RequestFeaturesTest {

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
    class UrlEncoding {

        @Test
        void shouldUrlEncodeQueryParamsByDefault() {
            stubFor(get(urlPathEqualTo("/search"))
                    .withQueryParam("q", equalTo("hello world"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"q\":\"hello world\"}")));

            client.given()
                    .queryParam("q", "hello world")
                    .when().get("/search")
                    .then()
                    .statusCode(200)
                    .body("q", isEqualTo("hello world"));
        }

        @Test
        void shouldSendRawParamsWhenUrlEncodingDisabled() {
            stubFor(get(urlEqualTo("/raw?q=already%20encoded"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"ok\":true}")));

            client.given()
                    .urlEncodingEnabled(false)
                    .queryParam("q", "already%20encoded")
                    .when().get("/raw")
                    .then()
                    .statusCode(200);
        }
    }

    @Nested
    class NoValueQueryParam {

        @Test
        void shouldSendQueryParamWithoutValue() {
            stubFor(get(urlEqualTo("/flags?verbose"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"verbose\":true}")));

            client.given()
                    .queryParam("verbose")
                    .when().get("/flags")
                    .then()
                    .statusCode(200);
        }

        @Test
        void shouldCombineNoValueParamWithRegularParam() {
            stubFor(get(urlEqualTo("/combo?debug&page=1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"ok\":true}")));

            client.given()
                    .queryParam("debug")
                    .queryParam("page", "1")
                    .when().get("/combo")
                    .then()
                    .statusCode(200);
        }
    }

    @Nested
    class GenericRequest {

        @Test
        void shouldDispatchGetViaRequestMethodEnum() {
            stubFor(get(urlEqualTo("/users"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"count\":5}")));

            client.given()
                    .when().request(HttpMethod.GET, "/users")
                    .then()
                    .statusCode(200)
                    .body("count", isEqualTo(5));
        }

        @Test
        void shouldDispatchPostViaRequestStringMethod() {
            stubFor(post(urlEqualTo("/users"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1}")));

            client.given()
                    .contentType(io.smallrye.httpassured.http.ContentType.JSON)
                    .body("{\"name\":\"Alice\"}")
                    .when().request("POST", "/users")
                    .then()
                    .statusCode(201)
                    .body("id", isEqualTo(1));
        }

        @Test
        void shouldDispatchGetWithPathParamsViaRequest() {
            stubFor(get(urlEqualTo("/users/7/posts"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"userId\":7}")));

            client.given()
                    .when().request(HttpMethod.GET, "/users/{id}/posts", 7)
                    .then()
                    .statusCode(200)
                    .body("userId", isEqualTo(7));
        }
    }

    @Nested
    class ConditionalResponseLogging {

        @Test
        void shouldLogWhenStatusCodeMatchesIfStatusCodeIsEqualTo() {
            stubFor(get(urlEqualTo("/not-found"))
                    .willReturn(aResponse().withStatus(404)
                            .withBody("{\"error\":\"not found\"}")));

            client.when().get("/not-found")
                    .then()
                    .log().ifStatusCodeIsEqualTo(404)
                    .statusCode(404);
        }

        @Test
        void shouldNotLogWhenStatusCodeDoesNotMatch() {
            stubFor(get(urlEqualTo("/ok"))
                    .willReturn(aResponse().withStatus(200)
                            .withBody("{\"ok\":true}")));

            client.when().get("/ok")
                    .then()
                    .log().ifStatusCodeIsEqualTo(404)
                    .statusCode(200);
        }

        @Test
        void shouldLogOnIfErrorWhenStatusIs4xx() {
            stubFor(get(urlEqualTo("/bad"))
                    .willReturn(aResponse().withStatus(400)
                            .withBody("{\"error\":\"bad request\"}")));

            client.when().get("/bad")
                    .then()
                    .log().ifError()
                    .statusCode(400);
        }

        @Test
        void shouldLogWhenPredicateMatches() {
            stubFor(get(urlEqualTo("/server-error"))
                    .willReturn(aResponse().withStatus(503)
                            .withBody("{\"error\":\"unavailable\"}")));

            client.when().get("/server-error")
                    .then()
                    .log().ifStatusCodeMatches(code -> code >= 500)
                    .statusCode(503);
        }
    }
}
