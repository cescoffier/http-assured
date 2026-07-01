package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.spec.RequestSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

/**
 * REST Assured compatibility tests -- {@code accept()} header shorthand.
 *
 * <p>Verifies that {@code accept(String)} and {@code accept(ContentType)} set
 * the {@code Accept} header on outgoing requests, matching REST Assured's API.
 */
@WireMockTest
class AcceptHeaderTest {

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
    void shouldSetAcceptHeaderFromString() {
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"ok\":true}")));

        client.given()
                .accept("application/json")
                .when().get("/test")
                .then()
                .statusCode(200);

        verify(getRequestedFor(urlEqualTo("/test"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void shouldSetAcceptHeaderFromContentType() {
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"ok\":true}")));

        client.given()
                .accept(ContentType.JSON)
                .when().get("/test")
                .then()
                .statusCode(200);

        verify(getRequestedFor(urlEqualTo("/test"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void shouldSetAcceptHeaderForXml() {
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<ok/>")));

        client.given()
                .accept(ContentType.XML)
                .when().get("/test")
                .then()
                .statusCode(200);

        verify(getRequestedFor(urlEqualTo("/test"))
                .withHeader("Accept", equalTo("application/xml")));
    }

    @Test
    void shouldSetAcceptHeaderForText() {
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("hello")));

        client.given()
                .accept(ContentType.TEXT)
                .when().get("/test")
                .then()
                .statusCode(200);

        verify(getRequestedFor(urlEqualTo("/test"))
                .withHeader("Accept", equalTo("text/plain")));
    }

    @Test
    void shouldOverrideAcceptHeader() {
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"ok\":true}")));

        client.given()
                .header("Accept", "text/plain")
                .accept("application/json")
                .when().get("/test")
                .then()
                .statusCode(200);

        verify(getRequestedFor(urlEqualTo("/test"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void shouldWorkInSpec() {
        stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"ok\":true}")));

        RequestSpec spec = RequestSpec.builder()
                .accept(ContentType.JSON)
                .build();

        client.given()
                .spec(spec)
                .when().get("/test")
                .then()
                .statusCode(200);

        verify(getRequestedFor(urlEqualTo("/test"))
                .withHeader("Accept", equalTo("application/json")));
    }
}
