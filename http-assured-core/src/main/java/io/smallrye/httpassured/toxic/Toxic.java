package io.smallrye.httpassured.toxic;

import java.time.Duration;

public sealed interface Toxic {

    // --- Factory methods ---

    static Toxic latency(Duration delay) {
        return new LatencyToxic(delay, Duration.ZERO);
    }

    static Toxic latency(Duration delay, Duration jitter) {
        return new LatencyToxic(delay, jitter);
    }

    static Toxic down() {
        return new DownToxic();
    }

    static Toxic bandwidth(long bytesPerSecond) {
        return new BandwidthToxic(bytesPerSecond);
    }

    static Toxic timeout(Duration timeout) {
        return new TimeoutToxic(timeout);
    }

    static Toxic resetPeer() {
        return new ResetPeerToxic(Duration.ZERO);
    }

    static Toxic resetPeer(Duration delay) {
        return new ResetPeerToxic(delay);
    }

    static Toxic slowClose(Duration delay) {
        return new SlowCloseToxic(delay);
    }

    static Toxic limitData(long bytes) {
        return new LimitDataToxic(bytes);
    }

    static Toxic respondWith(int statusCode) {
        return new RespondWithToxic(statusCode, null);
    }

    static Toxic respondWith(int statusCode, String body) {
        return new RespondWithToxic(statusCode, body);
    }

    // --- Modifiers ---

    default Toxic withToxicity(double toxicity) {
        return new ConfiguredToxic(this, toxicity, Stream.DOWNSTREAM);
    }

    default Toxic upstream() {
        return new ConfiguredToxic(this, 1.0, Stream.UPSTREAM);
    }

    // --- Stream direction ---

    enum Stream { UPSTREAM, DOWNSTREAM }

    // --- Variant records ---

    record LatencyToxic(Duration delay, Duration jitter) implements Toxic {}

    record DownToxic() implements Toxic {}

    record BandwidthToxic(long rateBytesPerSecond) implements Toxic {}

    record TimeoutToxic(Duration timeout) implements Toxic {}

    record ResetPeerToxic(Duration delay) implements Toxic {}

    record SlowCloseToxic(Duration delay) implements Toxic {}

    record LimitDataToxic(long bytes) implements Toxic {}

    record RespondWithToxic(int statusCode, String body) implements Toxic {}

    record ConfiguredToxic(Toxic inner, double toxicity, Stream stream) implements Toxic {
        @Override
        public Toxic withToxicity(double toxicity) {
            return new ConfiguredToxic(this.inner, toxicity, this.stream);
        }

        @Override
        public Toxic upstream() {
            return new ConfiguredToxic(this.inner, this.toxicity, Stream.UPSTREAM);
        }
    }
}
