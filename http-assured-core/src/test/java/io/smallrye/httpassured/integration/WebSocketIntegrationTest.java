package io.smallrye.httpassured.integration;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.HttpAssuredException;
import io.smallrye.httpassured.engine.vertx.VertxHttpEngine;
import io.smallrye.httpassured.websocket.WsSession;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for WebSocket support using an embedded Vert.x server.
 * WireMock does not support WebSocket protocol, so a lightweight Vert.x
 * HTTP server with WebSocket handler is used here.
 */
class WebSocketIntegrationTest {

    private static Vertx vertx;
    private static int port;
    private static HttpAssured client;

    @BeforeAll
    static void startServer() throws Exception {
        vertx = Vertx.vertx();

        port = vertx.createHttpServer()
                .webSocketHandler(ws -> {
                    String path = ws.path();
                    switch (path) {
                        case "/echo" -> {
                            ws.textMessageHandler(msg -> ws.writeTextMessage(msg));
                            ws.binaryMessageHandler(buf -> ws.writeBinaryMessage(buf));
                        }
                        case "/greeting" -> {
                            ws.writeTextMessage("welcome");
                            ws.textMessageHandler(msg -> ws.writeTextMessage("hello " + msg));
                        }
                        case "/stream" -> {
                            for (int i = 1; i <= 5; i++) {
                                final int num = i;
                                vertx.setTimer(10L * i, id -> ws.writeTextMessage("msg-" + num));
                            }
                            vertx.setTimer(100, id -> ws.close());
                        }
                        case "/close-immediately" -> ws.close((short) 1000, "bye");
                        default -> ws.close((short) 404);
                    }
                })
                .listen(0)
                .toCompletionStage()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS)
                .actualPort();

        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(port)
                .engine(new VertxHttpEngine(vertx))
                .build();
    }

    @AfterAll
    static void stopServer() {
        if (client != null) client.close();
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close().onComplete(v -> latch.countDown());
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Nested
    class EchoTests {

        @Test
        void shouldSendAndReceiveTextMessage() {
            try (WsSession session = client.webSocket("/echo").connect()) {
                session.sendText("hello");
                String reply = session.awaitText(Duration.ofSeconds(5));
                assertEquals("hello", reply);
            }
        }

        @Test
        void shouldSendAndReceiveBinaryMessage() {
            try (WsSession session = client.webSocket("/echo").connect()) {
                byte[] data = new byte[]{1, 2, 3, 4, 5};
                session.sendBinary(data);
                byte[] reply = session.awaitBinary(Duration.ofSeconds(5));
                assertArrayEquals(data, reply);
            }
        }

        @Test
        void shouldSendMultipleMessages() {
            try (WsSession session = client.webSocket("/echo").connect()) {
                session.sendText("first");
                assertEquals("first", session.awaitText(Duration.ofSeconds(5)));

                session.sendText("second");
                assertEquals("second", session.awaitText(Duration.ofSeconds(5)));

                session.sendText("third");
                assertEquals("third", session.awaitText(Duration.ofSeconds(5)));
            }
        }
    }

    @Nested
    class GreetingTests {

        @Test
        void shouldReceiveGreetingOnConnect() {
            try (WsSession session = client.webSocket("/greeting").connect()) {
                String welcome = session.awaitText(Duration.ofSeconds(5));
                assertEquals("welcome", welcome);
            }
        }

        @Test
        void shouldEchoWithPrefix() {
            try (WsSession session = client.webSocket("/greeting").connect()) {
                // Consume the welcome message
                session.awaitText(Duration.ofSeconds(5));

                session.sendText("world");
                String reply = session.awaitText(Duration.ofSeconds(5));
                assertEquals("hello world", reply);
            }
        }
    }

    @Nested
    class SessionLifecycle {

        @Test
        void shouldReportOpenState() {
            try (WsSession session = client.webSocket("/echo").connect()) {
                assertTrue(session.isOpen());
            }
        }

        @Test
        void shouldReportClosedAfterClose() {
            WsSession session = client.webSocket("/echo").connect();
            session.close();
            assertFalse(session.isOpen());
        }

        @Test
        void shouldCloseWithStatusCodeAndReason() {
            WsSession session = client.webSocket("/echo").connect();
            assertDoesNotThrow(() -> session.close(1000, "normal closure"));
        }

        @Test
        void shouldTimeoutWhenNoMessage() {
            try (WsSession session = client.webSocket("/echo").connect()) {
                assertThrows(HttpAssuredException.class,
                        () -> session.awaitText(Duration.ofMillis(100)));
            }
        }
    }

    @Nested
    class StreamingTests {

        @Test
        void shouldReceiveMultipleMessagesViaMulti() throws Exception {
            try (WsSession session = client.webSocket("/stream").connect()) {
                List<String> received = new ArrayList<>();
                CountDownLatch latch = new CountDownLatch(5);

                Multi<String> messages = session.textMessages();
                messages.subscribe().with(msg -> {
                    received.add(msg);
                    latch.countDown();
                });

                assertTrue(latch.await(5, TimeUnit.SECONDS),
                        "Did not receive all 5 messages within timeout");
                assertEquals(5, received.size());
                assertTrue(received.contains("msg-1"));
                assertTrue(received.contains("msg-5"));
            }
        }
    }

    @Nested
    class WebSocketBuilderTests {

        @Test
        void shouldConnectWithCustomHeaders() {
            try (WsSession session = client.webSocket("/echo")
                    .header("X-Custom", "test")
                    .connect()) {
                assertTrue(session.isOpen());
                session.sendText("ping");
                assertEquals("ping", session.awaitText(Duration.ofSeconds(5)));
            }
        }

        @Test
        void shouldConnectWithQueryParam() {
            try (WsSession session = client.webSocket("/echo")
                    .queryParam("room", "general")
                    .connect()) {
                assertTrue(session.isOpen());
            }
        }
    }
}
