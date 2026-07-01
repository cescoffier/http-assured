package io.smallrye.httpassured.toxic;

import io.smallrye.httpassured.HttpAssuredException;
import io.smallrye.httpassured.http.Headers;
import io.smallrye.httpassured.spi.HttpClientEngine;
import io.smallrye.httpassured.spi.RawResponse;
import io.smallrye.httpassured.spi.RequestContext;
import io.smallrye.httpassured.spi.WebSocketContext;
import io.smallrye.httpassured.websocket.WsSession;
import io.smallrye.mutiny.Uni;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

public class ToxicEngine implements HttpClientEngine {

    private final HttpClientEngine delegate;
    private final List<Toxic> toxics;

    public ToxicEngine(HttpClientEngine delegate, List<Toxic> toxics) {
        this.delegate = delegate;
        this.toxics = toxics;
    }

    @Override
    public RawResponse execute(RequestContext request) {
        return executeAsync(request)
                .await().indefinitely();
    }

    @Override
    public Uni<RawResponse> executeAsync(RequestContext request) {
        List<ResolvedToxic> active = resolveActive(toxics);

        // Short-circuit check: first short-circuit in list order wins
        for (ResolvedToxic rt : active) {
            Uni<RawResponse> shortCircuit = applyShortCircuit(rt);
            if (shortCircuit != null) {
                return shortCircuit;
            }
        }

        // Compute upstream latency
        Duration upstreamDelay = Duration.ZERO;
        for (ResolvedToxic rt : active) {
            if (rt.toxic() instanceof Toxic.LatencyToxic lt && rt.stream() == Toxic.Stream.UPSTREAM) {
                upstreamDelay = upstreamDelay.plus(computeLatency(lt));
            }
        }

        // Build pipeline: optional upstream delay → delegate → post-response toxics
        Uni<RawResponse> pipeline;
        if (!upstreamDelay.isZero()) {
            Duration finalDelay = upstreamDelay;
            pipeline = Uni.createFrom().voidItem()
                    .onItem().delayIt().by(finalDelay)
                    .onItem().transformToUni(ignored -> delegate.executeAsync(request));
        } else {
            pipeline = delegate.executeAsync(request);
        }

        for (ResolvedToxic rt : active) {
            pipeline = applyPostResponse(pipeline, rt);
        }
        return pipeline;
    }

    @Override
    public Uni<WsSession> openWebSocket(WebSocketContext context) {
        List<ResolvedToxic> active = resolveActive(toxics);

        for (ResolvedToxic rt : active) {
            Uni<WsSession> shortCircuit = applyWsShortCircuit(rt);
            if (shortCircuit != null) {
                return shortCircuit;
            }
        }

        // Compute upstream latency for WS
        Duration upstreamDelay = Duration.ZERO;
        for (ResolvedToxic rt : active) {
            if (rt.toxic() instanceof Toxic.LatencyToxic lt && rt.stream() == Toxic.Stream.UPSTREAM) {
                upstreamDelay = upstreamDelay.plus(computeLatency(lt));
            }
        }

        Uni<WsSession> pipeline;
        if (!upstreamDelay.isZero()) {
            Duration finalDelay = upstreamDelay;
            pipeline = Uni.createFrom().voidItem()
                    .onItem().delayIt().by(finalDelay)
                    .onItem().transformToUni(ignored -> delegate.openWebSocket(context));
        } else {
            pipeline = delegate.openWebSocket(context);
        }

        // Apply downstream latency and timeout
        for (ResolvedToxic rt : active) {
            if (rt.toxic() instanceof Toxic.LatencyToxic lt && rt.stream() == Toxic.Stream.DOWNSTREAM) {
                pipeline = pipeline.onItem().delayIt().by(computeLatency(lt));
            } else if (rt.toxic() instanceof Toxic.TimeoutToxic tt) {
                if (tt.timeout().isZero()) {
                    // Never completes
                    @SuppressWarnings("unchecked")
                    Uni<WsSession> nothing = (Uni<WsSession>) (Uni<?>) Uni.createFrom().nothing();
                    pipeline = nothing;
                } else {
                    pipeline = pipeline.ifNoItem().after(tt.timeout()).failWith(
                            () -> new HttpAssuredException("Request timed out (simulated by toxic)",
                                    new TimeoutException("Timed out after " + tt.timeout().toMillis() + "ms")));
                }
            } else if (rt.toxic() instanceof Toxic.ResetPeerToxic rp && !rp.delay().isZero()) {
                Uni<WsSession> resetAfterDelay = Uni.createFrom().<WsSession>emitter(em -> {
                    // This will never complete successfully, only fail after delay
                }).ifNoItem().after(rp.delay()).failWith(
                        () -> new HttpAssuredException(
                                "Connection reset by peer (simulated by toxic)",
                                new IOException("Connection reset")));
                pipeline = Uni.join().first(pipeline, resetAfterDelay).toTerminate();
            }
        }

        return pipeline;
    }

    @Override
    public void close() {
        // Don't close the delegate — we don't own it
    }

    // --- Internal helpers ---

    record ResolvedToxic(Toxic toxic, double toxicity, Toxic.Stream stream) {}

    private List<ResolvedToxic> resolveActive(List<Toxic> toxics) {
        List<ResolvedToxic> active = new ArrayList<>();
        for (Toxic toxic : toxics) {
            double toxicity = 1.0;
            Toxic.Stream stream = Toxic.Stream.DOWNSTREAM;
            Toxic inner = toxic;

            if (toxic instanceof Toxic.ConfiguredToxic ct) {
                toxicity = ct.toxicity();
                stream = ct.stream();
                inner = ct.inner();
            }

            if (toxicity >= 1.0 || ThreadLocalRandom.current().nextDouble() < toxicity) {
                active.add(new ResolvedToxic(inner, toxicity, stream));
            }
        }
        return active;
    }

    private Uni<RawResponse> applyShortCircuit(ResolvedToxic rt) {
        Toxic toxic = rt.toxic();

        if (toxic instanceof Toxic.DownToxic) {
            return Uni.createFrom().failure(
                    new HttpAssuredException("Connection refused (simulated by toxic)",
                            new ConnectException("Connection refused")));
        }

        if (toxic instanceof Toxic.RespondWithToxic rw) {
            byte[] body = rw.body() != null
                    ? rw.body().getBytes(StandardCharsets.UTF_8)
                    : new byte[0];
            return Uni.createFrom().item(new RawResponse(
                    rw.statusCode(), "Simulated", new Headers(), body, 0));
        }

        if (toxic instanceof Toxic.ResetPeerToxic rp && rp.delay().isZero()) {
            return Uni.createFrom().failure(
                    new HttpAssuredException(
                            "Connection reset by peer (simulated by toxic)",
                            new IOException("Connection reset")));
        }

        return null;
    }

    private Uni<RawResponse> applyPostResponse(Uni<RawResponse> pipeline, ResolvedToxic rt) {
        Toxic toxic = rt.toxic();

        // Short-circuits — already handled, pass through
        if (toxic instanceof Toxic.DownToxic) {
            return pipeline;
        }
        if (toxic instanceof Toxic.RespondWithToxic) {
            return pipeline;
        }
        if (toxic instanceof Toxic.ResetPeerToxic rp && rp.delay().isZero()) {
            return pipeline;
        }

        // Upstream latency — already handled before delegation
        if (toxic instanceof Toxic.LatencyToxic && rt.stream() == Toxic.Stream.UPSTREAM) {
            return pipeline;
        }

        // Downstream latency
        if (toxic instanceof Toxic.LatencyToxic lt) {
            return pipeline.onItem().delayIt().by(computeLatency(lt));
        }

        // Bandwidth: delay = body.length / rate
        if (toxic instanceof Toxic.BandwidthToxic bw) {
            return pipeline.onItem().transformToUni(response -> {
                int bodyLength = response.body() != null ? response.body().length : 0;
                if (bodyLength == 0 || bw.rateBytesPerSecond() <= 0) {
                    return Uni.createFrom().item(response);
                }
                long delayMs = (bodyLength * 1000L) / bw.rateBytesPerSecond();
                return Uni.createFrom().item(response)
                        .onItem().delayIt().by(Duration.ofMillis(delayMs));
            });
        }

        // Timeout: race delegate against timer
        if (toxic instanceof Toxic.TimeoutToxic tt) {
            if (tt.timeout().isZero()) {
                // Never completes - will timeout at the execute() level
                @SuppressWarnings("unchecked")
                Uni<RawResponse> nothing = (Uni<RawResponse>) (Uni<?>) Uni.createFrom().nothing();
                return nothing;
            }
            return pipeline.ifNoItem().after(tt.timeout()).failWith(
                    () -> new HttpAssuredException("Request timed out (simulated by toxic)",
                            new TimeoutException("Timed out after " + tt.timeout().toMillis() + "ms")));
        }

        // Delayed reset peer: race delegate against delayed failure
        if (toxic instanceof Toxic.ResetPeerToxic rp) {
            Uni<RawResponse> resetAfterDelay = Uni.createFrom().<RawResponse>emitter(em -> {
                // This will never complete successfully, only fail after delay
            }).ifNoItem().after(rp.delay()).failWith(
                    () -> new HttpAssuredException(
                            "Connection reset by peer (simulated by toxic)",
                            new IOException("Connection reset")));
            return Uni.join().first(pipeline, resetAfterDelay).toTerminate();
        }

        // Slow close: add delay after response
        if (toxic instanceof Toxic.SlowCloseToxic sc) {
            return pipeline.onItem().delayIt().by(sc.delay());
        }

        // Limit data: check body length
        if (toxic instanceof Toxic.LimitDataToxic ld) {
            return pipeline.onItem().transformToUni(response -> {
                int bodyLength = response.body() != null ? response.body().length : 0;
                if (bodyLength > ld.bytes()) {
                    return Uni.createFrom().failure(
                            new HttpAssuredException(
                                    "Connection closed: data limit exceeded (simulated by toxic)",
                                    new IOException("Data limit exceeded: " + bodyLength + " > " + ld.bytes())));
                }
                return Uni.createFrom().item(response);
            });
        }

        // ConfiguredToxic should not appear after unwrap
        return pipeline;
    }

    private <T> Uni<T> applyWsShortCircuit(ResolvedToxic rt) {
        Toxic toxic = rt.toxic();

        if (toxic instanceof Toxic.DownToxic) {
            return Uni.createFrom().failure(
                    new HttpAssuredException("Connection refused (simulated by toxic)",
                            new ConnectException("Connection refused")));
        }

        if (toxic instanceof Toxic.ResetPeerToxic rp && rp.delay().isZero()) {
            return Uni.createFrom().failure(
                    new HttpAssuredException(
                            "Connection reset by peer (simulated by toxic)",
                            new IOException("Connection reset")));
        }

        return null;
    }

    private Duration computeLatency(Toxic.LatencyToxic lt) {
        long delayMs = lt.delay().toMillis();
        long jitterMs = lt.jitter().toMillis();
        if (jitterMs > 0) {
            delayMs += ThreadLocalRandom.current().nextLong(-jitterMs, jitterMs + 1);
            delayMs = Math.max(0, delayMs);
        }
        return Duration.ofMillis(delayMs);
    }
}
