package io.smallrye.httpassured.toxic;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.HttpAssuredException;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.Headers;
import io.smallrye.httpassured.http.HttpMethod;
import io.smallrye.httpassured.spi.HttpClientEngine;
import io.smallrye.httpassured.spi.RawResponse;
import io.smallrye.httpassured.spi.RequestContext;
import io.smallrye.httpassured.spi.WebSocketContext;
import io.smallrye.httpassured.websocket.WsSession;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ToxicEngineTest {

    private static final RawResponse CANNED_RESPONSE = new RawResponse(
            200, "OK", new Headers(), "hello".getBytes(), 10);

    private static RequestContext simpleRequest() {
        return RequestContext.builder()
                .method(HttpMethod.GET)
                .uri("http://localhost:8080/test")
                .build();
    }

    private static HttpClientEngine fakeEngine(AtomicBoolean called) {
        return new HttpClientEngine() {
            @Override
            public RawResponse execute(RequestContext request) {
                called.set(true);
                return CANNED_RESPONSE;
            }

            @Override
            public Uni<RawResponse> executeAsync(RequestContext request) {
                called.set(true);
                return Uni.createFrom().item(CANNED_RESPONSE);
            }

            @Override
            public Uni<WsSession> openWebSocket(WebSocketContext context) {
                called.set(true);
                return Uni.createFrom().nullItem();
            }

            @Override
            public void close() {}
        };
    }

    @Test
    void downToxicFailsWithoutDelegating() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(fakeEngine(delegateCalled), List.of(Toxic.down()));

        HttpAssuredException ex = assertThrows(HttpAssuredException.class,
                () -> engine.execute(simpleRequest()));
        assertInstanceOf(ConnectException.class, ex.getCause());
        assertTrue(ex.getMessage().contains("simulated by toxic"));
        assertFalse(delegateCalled.get());
    }

    @Test
    void respondWithReturnssyntheticResponseWithoutDelegating() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.respondWith(503, "Service Unavailable")));

        RawResponse response = engine.execute(simpleRequest());
        assertEquals(503, response.statusCode());
        assertEquals("Service Unavailable", response.bodyAsString());
        assertFalse(delegateCalled.get());
    }

    @Test
    void respondWithStatusOnlyReturnsEmptyBody() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.respondWith(404)));

        RawResponse response = engine.execute(simpleRequest());
        assertEquals(404, response.statusCode());
        assertEquals("", response.bodyAsString());
        assertFalse(delegateCalled.get());
    }

    @Test
    void immediateResetPeerFailsWithoutDelegating() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled), List.of(Toxic.resetPeer()));

        HttpAssuredException ex = assertThrows(HttpAssuredException.class,
                () -> engine.execute(simpleRequest()));
        assertTrue(ex.getMessage().contains("Connection reset"));
        assertTrue(ex.getMessage().contains("simulated by toxic"));
        assertFalse(delegateCalled.get());
    }

    @Test
    void firstShortCircuitWinsInListOrder() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        // respondWith comes before down — respondWith should win
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.respondWith(418), Toxic.down()));

        RawResponse response = engine.execute(simpleRequest());
        assertEquals(418, response.statusCode());
        assertFalse(delegateCalled.get());
    }

    @Test
    void noToxicsDelegatesDirectly() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(fakeEngine(delegateCalled), List.of());

        RawResponse response = engine.execute(simpleRequest());
        assertEquals(200, response.statusCode());
        assertTrue(delegateCalled.get());
    }

    @Test
    void downstreamLatencyAddsDelay() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.latency(Duration.ofMillis(200))));

        long start = System.nanoTime();
        RawResponse response = engine.execute(simpleRequest());
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertEquals(200, response.statusCode());
        assertTrue(delegateCalled.get());
        assertTrue(elapsed >= 150, "Expected at least 150ms delay, got " + elapsed + "ms");
    }

    @Test
    void upstreamLatencyAddsDelayBeforeDelegating() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.latency(Duration.ofMillis(200)).upstream()));

        long start = System.nanoTime();
        RawResponse response = engine.execute(simpleRequest());
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertEquals(200, response.statusCode());
        assertTrue(delegateCalled.get());
        assertTrue(elapsed >= 150, "Expected at least 150ms delay, got " + elapsed + "ms");
    }

    @Test
    void bandwidthAddsDelayProportionalToBodySize() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        // 5 bytes at 50 bytes/sec = 100ms
        HttpClientEngine slowEngine = new HttpClientEngine() {
            @Override public RawResponse execute(RequestContext r) { return executeAsync(r).await().atMost(Duration.ofSeconds(5)); }
            @Override public Uni<RawResponse> executeAsync(RequestContext r) {
                delegateCalled.set(true);
                return Uni.createFrom().item(new RawResponse(200, "OK", new Headers(), "hello".getBytes(), 1));
            }
            @Override public Uni<WsSession> openWebSocket(WebSocketContext c) { return Uni.createFrom().nullItem(); }
            @Override public void close() {}
        };

        ToxicEngine engine = new ToxicEngine(slowEngine, List.of(Toxic.bandwidth(50)));

        long start = System.nanoTime();
        engine.execute(simpleRequest());
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertTrue(delegateCalled.get());
        assertTrue(elapsed >= 80, "Expected at least 80ms delay for bandwidth throttle, got " + elapsed + "ms");
    }

    @Test
    void timeoutFailsWhenDelegateTooSlow() {
        ToxicEngine engine = new ToxicEngine(
                new HttpClientEngine() {
                    @Override public RawResponse execute(RequestContext r) { return executeAsync(r).await().atMost(Duration.ofSeconds(5)); }
                    @Override public Uni<RawResponse> executeAsync(RequestContext r) {
                        return Uni.createFrom().item(CANNED_RESPONSE)
                                .onItem().delayIt().by(Duration.ofSeconds(2));
                    }
                    @Override public Uni<WsSession> openWebSocket(WebSocketContext c) { return Uni.createFrom().nullItem(); }
                    @Override public void close() {}
                },
                List.of(Toxic.timeout(Duration.ofMillis(100))));

        HttpAssuredException ex = assertThrows(HttpAssuredException.class,
                () -> engine.execute(simpleRequest()));
        assertTrue(ex.getMessage().contains("timed out"));
        assertTrue(ex.getMessage().contains("simulated by toxic"));
    }

    @Test
    void timeoutPassesWhenDelegateIsFastEnough() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.timeout(Duration.ofSeconds(5))));

        RawResponse response = engine.execute(simpleRequest());
        assertEquals(200, response.statusCode());
        assertTrue(delegateCalled.get());
    }

    @Test
    void delayedResetPeerFailsAfterDelay() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                new HttpClientEngine() {
                    @Override public RawResponse execute(RequestContext r) { return executeAsync(r).await().atMost(Duration.ofSeconds(5)); }
                    @Override public Uni<RawResponse> executeAsync(RequestContext r) {
                        delegateCalled.set(true);
                        return Uni.createFrom().item(CANNED_RESPONSE)
                                .onItem().delayIt().by(Duration.ofSeconds(2));
                    }
                    @Override public Uni<WsSession> openWebSocket(WebSocketContext c) { return Uni.createFrom().nullItem(); }
                    @Override public void close() {}
                },
                List.of(Toxic.resetPeer(Duration.ofMillis(100))));

        HttpAssuredException ex = assertThrows(HttpAssuredException.class,
                () -> engine.execute(simpleRequest()));
        assertTrue(ex.getMessage().contains("Connection reset"));
    }

    @Test
    void slowCloseAddsDelay() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.slowClose(Duration.ofMillis(200))));

        long start = System.nanoTime();
        RawResponse response = engine.execute(simpleRequest());
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertEquals(200, response.statusCode());
        assertTrue(elapsed >= 150, "Expected at least 150ms delay, got " + elapsed + "ms");
    }

    @Test
    void limitDataPassesWhenUnderLimit() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.limitData(1024)));

        RawResponse response = engine.execute(simpleRequest());
        assertEquals(200, response.statusCode());
        assertTrue(delegateCalled.get());
    }

    @Test
    void limitDataFailsWhenOverLimit() {
        ToxicEngine engine = new ToxicEngine(
                new HttpClientEngine() {
                    @Override public RawResponse execute(RequestContext r) { return executeAsync(r).await().atMost(Duration.ofSeconds(5)); }
                    @Override public Uni<RawResponse> executeAsync(RequestContext r) {
                        byte[] bigBody = new byte[2048];
                        return Uni.createFrom().item(new RawResponse(200, "OK", new Headers(), bigBody, 1));
                    }
                    @Override public Uni<WsSession> openWebSocket(WebSocketContext c) { return Uni.createFrom().nullItem(); }
                    @Override public void close() {}
                },
                List.of(Toxic.limitData(1024)));

        HttpAssuredException ex = assertThrows(HttpAssuredException.class,
                () -> engine.execute(simpleRequest()));
        assertTrue(ex.getMessage().contains("data limit exceeded"));
    }

    @Test
    void toxicityZeroNeverApplies() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(Toxic.down().withToxicity(0.0)));

        // Should NOT throw — toxicity=0.0 means the toxic never fires
        RawResponse response = engine.execute(simpleRequest());
        assertEquals(200, response.statusCode());
        assertTrue(delegateCalled.get());
    }

    @Test
    void multiplePostResponseToxicsCompose() {
        AtomicBoolean delegateCalled = new AtomicBoolean(false);
        // latency 100ms + slowClose 100ms = ~200ms total
        ToxicEngine engine = new ToxicEngine(
                fakeEngine(delegateCalled),
                List.of(
                        Toxic.latency(Duration.ofMillis(100)),
                        Toxic.slowClose(Duration.ofMillis(100))));

        long start = System.nanoTime();
        RawResponse response = engine.execute(simpleRequest());
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertEquals(200, response.statusCode());
        assertTrue(elapsed >= 150, "Expected at least 150ms combined delay, got " + elapsed + "ms");
    }

    @Test
    void fullDslPathWithRespondWithToxic() {
        // respondWith should short-circuit — no real server needed
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:1") // invalid port, should never connect
                .build();
        try {
            Response response = client.given()
                    .toxic(Toxic.respondWith(503, "injected"))
                    .when()
                    .get("/anything");
            assertEquals(503, response.statusCode());
            assertEquals("injected", response.bodyAsString());
        } finally {
            client.close();
        }
    }
}
