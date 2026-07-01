package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;

/**
 * REST Assured compatibility tests -- Content-Type handling.
 *
 * <p>Verifies content type assertion on responses, content type setting on
 * requests, auto-detection for object bodies, and charset tolerance.
 */
@WireMockTest
class ContentTypeTest {

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
    void shouldAssertResponseContentType() {
        stubFor(get(urlEqualTo("/json-endpoint"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));

        client.when().get("/json-endpoint")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("status", isEqualTo("ok"));
    }

    @Test
    void shouldSetRequestContentType() {
        stubFor(post(urlEqualTo("/typed-post"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"created\":true}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"test\"}")
                .when().post("/typed-post")
                .then()
                .statusCode(201)
                .body("created", isEqualTo(true));
    }

    @Test
    void shouldAutoDetectJsonContentTypeForObjectBody() {
        stubFor(post(urlEqualTo("/auto-json"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"received\":true}")));

        // body(Object) auto-sets Content-Type to JSON when no explicit contentType is set
        Item item = new Item();
        item.name = "widget";
        item.quantity = 5;

        client.given()
                .body(item)
                .when().post("/auto-json")
                .then()
                .statusCode(200)
                .body("received", isEqualTo(true));
    }

    @Test
    void shouldAssertContentTypeWithCharset() {
        stubFor(get(urlEqualTo("/charset-json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                        .withBody("{\"message\":\"hello\"}")));

        // contentType() uses headerContains, so "application/json" matches even
        // when the response includes a charset parameter
        client.when().get("/charset-json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", isEqualTo("hello"));
    }

    public static class Item {
        public String name;
        public int quantity;

        public Item() {}
    }
}
