package io.smallrye.httpassured.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures fault injection for a test method.
 * <p>
 * Can be repeated to apply multiple toxics.
 * </p>
 * <pre>{@code
 * @Test
 * @WithToxic(latencyMs = 500)
 * void shouldHandleSlowResponses(HttpAssured client) { ... }
 *
 * @Test
 * @WithToxic(respondWithStatus = 418)
 * void shouldHandleCustomResponse(HttpAssured client) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithToxics.class)
public @interface WithToxic {

    /**
     * Adds latency in milliseconds. 0 means no latency toxic.
     */
    long latencyMs() default 0;

    /**
     * Adds jitter in milliseconds (used with latencyMs). 0 means no jitter.
     */
    long jitterMs() default 0;

    /**
     * Limits bandwidth in bytes per second. 0 means no bandwidth toxic.
     */
    long bandwidthBytesPerSecond() default 0;

    /**
     * Simulates connection down (connection refused).
     */
    boolean down() default false;

    /**
     * Simulates a connection reset by peer.
     */
    boolean resetPeer() default false;

    /**
     * Optional delay in milliseconds before reset peer (used with resetPeer). 0 means immediate reset.
     */
    long resetPeerDelayMs() default 0;

    /**
     * Simulates a timeout in milliseconds. 0 means no timeout toxic.
     */
    long timeoutMs() default 0;

    /**
     * Simulates slow connection close in milliseconds. 0 means no slow close toxic.
     */
    long slowCloseMs() default 0;

    /**
     * Limits data transfer to specified bytes. 0 means no limit toxic.
     */
    long limitDataBytes() default 0;

    /**
     * Responds with a custom status code instead of making real request. 0 means no respond toxic.
     */
    int respondWithStatus() default 0;

    /**
     * Probability (0.0 to 1.0) that the toxic will be applied. Default is 1.0 (always applied).
     */
    double toxicity() default 1.0;
}
