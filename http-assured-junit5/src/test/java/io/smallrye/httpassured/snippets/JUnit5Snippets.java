package io.smallrye.httpassured.snippets;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.junit5.HttpTest;
import io.smallrye.httpassured.junit5.WebSocket;
import io.smallrye.httpassured.junit5.WithToxic;
import io.smallrye.httpassured.websocket.WsSession;

/**
 * Code snippets for JUnit 5 extension documentation.
 * This class compiles against the actual junit5 module annotations.
 */
@SuppressWarnings("unused")
public class JUnit5Snippets {

// tag::http-test-annotation[]
@HttpTest(baseUri = "http://localhost", port = 8080, basePath = "/api")
class UserApiTest {

    @Test
    void shouldGetUser(HttpAssured client) {
        client.when().get("/users/1")
            .then()
                .statusCode(200)
                .body("name", Assertions.isNotNull());
    }
}
// end::http-test-annotation[]

// tag::with-toxic-annotation[]
@HttpTest(baseUri = "http://localhost", port = 8080)
class ResilienceTest {

    @Test
    @WithToxic(latencyMs = 2000)
    void shouldHandleSlowResponses(HttpAssured client) {
        client.when().get("/api/data")
            .then()
                .statusCode(200);
    }

    @Test
    @WithToxic(respondWithStatus = 503)
    void shouldHandleServiceUnavailable(HttpAssured client) {
        client.when().get("/api/data")
            .then()
                .statusCode(503);
    }

    @Test
    @WithToxic(down = true)
    void shouldHandleConnectionDown(HttpAssured client) {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
            client.when().get("/api/data"));
    }
}
// end::with-toxic-annotation[]

// tag::multiple-toxics[]
@Test
@WithToxic(latencyMs = 500)
@WithToxic(bandwidthBytesPerSecond = 1024)
void shouldHandleSlowAndThrottledConnection(HttpAssured client) {
    client.when().get("/api/data")
        .then()
            .statusCode(200);
}
// end::multiple-toxics[]

// tag::combining-annotations[]
@HttpTest(baseUri = "http://localhost", port = 8080)
class FullIntegrationTest {

    @Test
    @WithToxic(latencyMs = 500)
    void shouldHandleSlowApi(HttpAssured client) {
        client.when().get("/api/health")
            .then()
                .statusCode(200);
    }

    @Test
    void shouldEchoOverWebSocket(
            HttpAssured client,
            @WebSocket("/ws") WsSession session) {
        session.sendText("ping");
    }
}
// end::combining-annotations[]

// tag::websocket-annotation[]
@HttpTest(baseUri = "http://localhost", port = 8080)
class WebSocketTest {

    @Test
    void shouldEchoMessage(
            HttpAssured client,
            @WebSocket("/echo") WsSession session) {
        session.sendText("hello");
    }
}
// end::websocket-annotation[]

}
