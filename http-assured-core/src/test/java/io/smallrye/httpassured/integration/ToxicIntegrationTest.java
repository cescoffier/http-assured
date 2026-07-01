package io.smallrye.httpassured.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.HttpAssuredException;
import io.smallrye.httpassured.toxic.Toxic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class ToxicIntegrationTest {

    private HttpAssured client;

    @BeforeEach
    void setup(WireMockRuntimeInfo wmInfo) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .build();
    }

    @AfterEach
    void teardown() {
        if (client != null) client.close();
    }

    @Test
    void latencyAddsDelayToRealRequest() {
        stubFor(get(urlEqualTo("/fast"))
                .willReturn(aResponse().withStatus(200).withBody("ok")));

        long start = System.nanoTime();
        client.given()
                .toxic(Toxic.latency(Duration.ofMillis(200)))
                .when()
                .get("/fast")
                .then()
                .statusCode(200)
                .bodyEquals("ok");
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsed >= 150, "Expected >= 150ms with latency toxic, got " + elapsed + "ms");
    }

    @Test
    void downToxicPreventsRealRequest() {
        stubFor(get(urlEqualTo("/down"))
                .willReturn(aResponse().withStatus(200).withBody("should not reach")));

        assertThrows(HttpAssuredException.class, () ->
                client.given()
                        .toxic(Toxic.down())
                        .when()
                        .get("/down"));
    }

    @Test
    void respondWithBypassesRealServer() {
        // No WireMock stub needed — respondWith short-circuits
        var response = client.given()
                .toxic(Toxic.respondWith(503, "injected fault"))
                .when()
                .get("/no-stub-needed");

        assertEquals(503, response.statusCode());
        assertEquals("injected fault", response.bodyAsString());
    }

    @Test
    void timeoutWithSlowServer() {
        stubFor(get(urlEqualTo("/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("slow response")
                        .withFixedDelay(2000)));

        assertThrows(HttpAssuredException.class, () ->
                client.given()
                        .toxic(Toxic.timeout(Duration.ofMillis(200)))
                        .when()
                        .get("/slow"));
    }

    @Test
    void limitDataWithLargeResponse() {
        byte[] largeBody = new byte[5000];
        java.util.Arrays.fill(largeBody, (byte) 'x');
        stubFor(get(urlEqualTo("/large"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(new String(largeBody))));

        assertThrows(HttpAssuredException.class, () ->
                client.given()
                        .toxic(Toxic.limitData(1024))
                        .when()
                        .get("/large"));
    }

    @Test
    void requestWithoutToxicsWorksNormally() {
        stubFor(get(urlEqualTo("/normal"))
                .willReturn(aResponse().withStatus(200).withBody("normal")));

        client.given()
                .when()
                .get("/normal")
                .then()
                .statusCode(200)
                .bodyEquals("normal");
    }
}
