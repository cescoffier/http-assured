package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

/**
 * REST Assured compatibility tests -- request body variants.
 *
 * <p>Covers {@code body(InputStream)} (read raw bytes, no auto Content-Type)
 * and {@code body(Map)} (serialized to JSON via Jackson, auto Content-Type).
 */
@WireMockTest
class RequestBodyTest {

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

    // body(InputStream)

    @Test
    void shouldSendBodyFromInputStream() {
        String json = "{\"name\":\"Alice\"}";

        stubFor(post(urlEqualTo("/stream"))
                .withRequestBody(equalToJson(json))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .contentType(ContentType.JSON)
                .body(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                .when().post("/stream")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldSendBodyFromInputStreamWithExplicitContentType() {
        String json = "{\"key\":\"value\"}";

        stubFor(post(urlEqualTo("/stream-typed"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(json))
                .willReturn(aResponse().withStatus(201)));

        client.given()
                .contentType(ContentType.JSON)
                .body(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                .when().post("/stream-typed")
                .then()
                .statusCode(201);
    }

    @Test
    void shouldNotAutoSetContentTypeForInputStream() {
        String payload = "plain text data";

        stubFor(post(urlEqualTo("/stream-no-ct"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .body(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)))
                .when().post("/stream-no-ct")
                .then()
                .statusCode(200);

        // Verify no Content-Type header was sent
        verify(postRequestedFor(urlEqualTo("/stream-no-ct"))
                .withoutHeader("Content-Type"));
    }

    // body(Map) — should serialize to JSON via body(Object) path

    @Test
    void shouldSendMapAsJsonBody() {
        stubFor(post(urlEqualTo("/map"))
                .withRequestBody(equalToJson("{\"name\":\"John\",\"age\":30}", true, false))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .body(Map.of("name", "John", "age", 30))
                .when().post("/map")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldAutoSetContentTypeForMap() {
        stubFor(post(urlEqualTo("/map-ct"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .body(Map.of("status", "ok"))
                .when().post("/map-ct")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldSendNestedMapAsJson() {
        stubFor(post(urlEqualTo("/nested-map"))
                .withRequestBody(equalToJson("{\"user\":{\"name\":\"John\"}}", true, false))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .body(Map.of("user", Map.of("name", "John")))
                .when().post("/nested-map")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldSendMapWithListValues() {
        stubFor(post(urlEqualTo("/map-list"))
                .withRequestBody(equalToJson("{\"items\":[1,2,3]}", true, false))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .body(Map.of("items", List.of(1, 2, 3)))
                .when().post("/map-list")
                .then()
                .statusCode(200);
    }
}
