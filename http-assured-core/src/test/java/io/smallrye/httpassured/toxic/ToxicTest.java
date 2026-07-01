package io.smallrye.httpassured.toxic;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ToxicTest {

    @Test
    void latencyWithDelayOnly() {
        Toxic toxic = Toxic.latency(Duration.ofMillis(500));
        assertInstanceOf(Toxic.LatencyToxic.class, toxic);
        var latency = (Toxic.LatencyToxic) toxic;
        assertEquals(Duration.ofMillis(500), latency.delay());
        assertEquals(Duration.ZERO, latency.jitter());
    }

    @Test
    void latencyWithJitter() {
        Toxic toxic = Toxic.latency(Duration.ofMillis(500), Duration.ofMillis(100));
        var latency = (Toxic.LatencyToxic) toxic;
        assertEquals(Duration.ofMillis(500), latency.delay());
        assertEquals(Duration.ofMillis(100), latency.jitter());
    }

    @Test
    void down() {
        Toxic toxic = Toxic.down();
        assertInstanceOf(Toxic.DownToxic.class, toxic);
    }

    @Test
    void bandwidth() {
        Toxic toxic = Toxic.bandwidth(10_000);
        assertInstanceOf(Toxic.BandwidthToxic.class, toxic);
        assertEquals(10_000, ((Toxic.BandwidthToxic) toxic).rateBytesPerSecond());
    }

    @Test
    void timeout() {
        Toxic toxic = Toxic.timeout(Duration.ofSeconds(5));
        assertInstanceOf(Toxic.TimeoutToxic.class, toxic);
        assertEquals(Duration.ofSeconds(5), ((Toxic.TimeoutToxic) toxic).timeout());
    }

    @Test
    void resetPeerImmediate() {
        Toxic toxic = Toxic.resetPeer();
        assertInstanceOf(Toxic.ResetPeerToxic.class, toxic);
        assertEquals(Duration.ZERO, ((Toxic.ResetPeerToxic) toxic).delay());
    }

    @Test
    void resetPeerWithDelay() {
        Toxic toxic = Toxic.resetPeer(Duration.ofMillis(200));
        assertEquals(Duration.ofMillis(200), ((Toxic.ResetPeerToxic) toxic).delay());
    }

    @Test
    void slowClose() {
        Toxic toxic = Toxic.slowClose(Duration.ofMillis(300));
        assertInstanceOf(Toxic.SlowCloseToxic.class, toxic);
        assertEquals(Duration.ofMillis(300), ((Toxic.SlowCloseToxic) toxic).delay());
    }

    @Test
    void limitData() {
        Toxic toxic = Toxic.limitData(1024);
        assertInstanceOf(Toxic.LimitDataToxic.class, toxic);
        assertEquals(1024, ((Toxic.LimitDataToxic) toxic).bytes());
    }

    @Test
    void respondWithStatusOnly() {
        Toxic toxic = Toxic.respondWith(503);
        assertInstanceOf(Toxic.RespondWithToxic.class, toxic);
        var rw = (Toxic.RespondWithToxic) toxic;
        assertEquals(503, rw.statusCode());
        assertNull(rw.body());
    }

    @Test
    void respondWithStatusAndBody() {
        Toxic toxic = Toxic.respondWith(503, "Service Unavailable");
        var rw = (Toxic.RespondWithToxic) toxic;
        assertEquals(503, rw.statusCode());
        assertEquals("Service Unavailable", rw.body());
    }

    @Test
    void withToxicityWrapsInConfiguredToxic() {
        Toxic base = Toxic.down();
        Toxic configured = base.withToxicity(0.5);
        assertInstanceOf(Toxic.ConfiguredToxic.class, configured);
        var ct = (Toxic.ConfiguredToxic) configured;
        assertEquals(0.5, ct.toxicity());
        assertSame(base, ct.inner());
        assertEquals(Toxic.Stream.DOWNSTREAM, ct.stream());
    }

    @Test
    void upstreamWrapsInConfiguredToxic() {
        Toxic base = Toxic.latency(Duration.ofMillis(100));
        Toxic configured = base.upstream();
        assertInstanceOf(Toxic.ConfiguredToxic.class, configured);
        var ct = (Toxic.ConfiguredToxic) configured;
        assertEquals(Toxic.Stream.UPSTREAM, ct.stream());
        assertEquals(1.0, ct.toxicity());
        assertSame(base, ct.inner());
    }

    @Test
    void chainingWithToxicityThenUpstreamPreservesBoth() {
        Toxic base = Toxic.latency(Duration.ofMillis(100));
        Toxic configured = base.withToxicity(0.3).upstream();
        assertInstanceOf(Toxic.ConfiguredToxic.class, configured);
        var ct = (Toxic.ConfiguredToxic) configured;
        assertEquals(Toxic.Stream.UPSTREAM, ct.stream());
        assertEquals(0.3, ct.toxicity());
    }

    @Test
    void chainingUpstreamThenWithToxicityPreservesBoth() {
        Toxic base = Toxic.latency(Duration.ofMillis(100));
        Toxic configured = base.upstream().withToxicity(0.7);
        assertInstanceOf(Toxic.ConfiguredToxic.class, configured);
        var ct = (Toxic.ConfiguredToxic) configured;
        assertEquals(Toxic.Stream.UPSTREAM, ct.stream());
        assertEquals(0.7, ct.toxicity());
    }
}
