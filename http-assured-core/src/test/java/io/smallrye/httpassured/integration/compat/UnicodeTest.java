package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;

/**
 * REST Assured compatibility tests — UTF-8 / Unicode handling.
 *
 * <p>Verifies that JSON bodies, request payloads, and query parameters
 * correctly round-trip multi-byte Unicode characters (CJK, emoji, accented).
 */
@WireMockTest
class UnicodeTest {

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
    void shouldHandleUtf8InJsonResponseBody() {
        stubFor(get(urlEqualTo("/unicode"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                        .withBody("{\"name\":\"日本語\",\"emoji\":\"🎉\"}")));

        client.given()
                .when().get("/unicode")
                .then()
                .statusCode(200)
                .body("name", isEqualTo("日本語"))
                .body("emoji", isEqualTo("🎉"));
    }

    @Test
    void shouldHandleUtf8InJsonRequestBody() {
        stubFor(post(urlEqualTo("/data"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"日本語\",\"city\":\"Zürich\"}")
                .when().post("/data")
                .then()
                .statusCode(201);

        verify(postRequestedFor(urlEqualTo("/data"))
                .withRequestBody(containing("日本語")));
    }

    @Test
    void shouldHandleUtf8InQueryParam() {
        stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("café"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"query\":\"café\",\"results\":0}")));

        client.given()
                .queryParam("q", "café")
                .when().get("/search")
                .then()
                .statusCode(200)
                .body("query", isEqualTo("café"));
    }
}
