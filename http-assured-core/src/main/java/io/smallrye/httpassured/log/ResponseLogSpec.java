package io.smallrye.httpassured.log;

import io.smallrye.httpassured.spi.RawResponse;

import java.util.EnumSet;
import java.util.Set;

/**
 * Fluent specification for response-side logging.
 * <p>
 * Obtained via {@link io.smallrye.httpassured.dsl.ValidatableResponse#log()} or
 * {@link io.smallrye.httpassured.dsl.Response#log()}. Each method selects what to
 * log and returns the owning parent, keeping the DSL chain fluent:
 * </p>
 * <pre>{@code
 * // Always log everything
 * client.when().get("/users")
 *     .then()
 *     .log().all()
 *     .statusCode(200);
 *
 * // Log only if status >= 400
 * client.when().get("/users")
 *     .then()
 *     .log().ifError()
 *     .statusCode(200);
 *
 * // Log only if an assertion fails
 * client.when().get("/users")
 *     .then()
 *     .log().ifValidationFails()
 *     .statusCode(200);
 * }</pre>
 * <p>
 * {@code ifValidationFails()} is available only on {@code ValidatableResponse}.
 * {@code Response.log()} supports {@code all()}, {@code body()}, {@code headers()},
 * {@code status()}, and {@code ifError()}.
 * </p>
 * <p>
 * Logging uses JBoss Logging at INFO level for unconditional variants, and ERROR
 * level for {@code ifError} and {@code ifValidationFails}. Headers in the configured
 * blacklist are printed as {@code [ BLACKLISTED ]}.
 * </p>
 *
 * @param <P> the parent type ({@code ValidatableResponse} or {@code Response})
 */
public final class ResponseLogSpec<P> {

    /** Fields that can appear in a response log entry. */
    public enum Field {
        STATUS, HEADERS, BODY
    }

    /**
     * Callback invoked by {@link #ifValidationFails()} to activate deferred logging
     * on the owning {@code ValidatableResponse}. {@code null} when the parent is a
     * plain {@code Response} (where deferred logging is not supported).
     */
    @FunctionalInterface
    public interface OnFailureActivator {
        /** Activates deferred logging and returns the parent {@code ValidatableResponse}. */
        void activate(Set<String> blacklist);
    }

    private final P owner;
    private final RawResponse raw;
    private final Set<String> blacklist;
    private final OnFailureActivator onFailureActivator;

    /**
     * Created only by {@code ValidatableResponse.log()} and {@code Response.log()}.
     *
     * @param owner              the owning response object
     * @param raw                the raw HTTP response to log from
     * @param blacklist          case-insensitive set of header names to mask
     * @param onFailureActivator callback that activates deferred logging on the parent
     *                           (may be {@code null} for plain {@code Response})
     */
    public ResponseLogSpec(P owner, RawResponse raw, Set<String> blacklist, OnFailureActivator onFailureActivator) {
        this.owner = owner;
        this.raw = raw;
        this.blacklist = blacklist;
        this.onFailureActivator = onFailureActivator;
    }

    /**
     * Logs status, all headers, and body at INFO level.
     *
     * @return the owning parent
     */
    public P all() {
        ResponseLogger.log(raw, EnumSet.allOf(Field.class), blacklist);
        return owner;
    }

    /**
     * Logs only the response body at INFO level.
     *
     * @return the owning parent
     */
    public P body() {
        ResponseLogger.log(raw, EnumSet.of(Field.BODY), blacklist);
        return owner;
    }

    /**
     * Logs only the response headers at INFO level (sensitive values are masked).
     *
     * @return the owning parent
     */
    public P headers() {
        ResponseLogger.log(raw, EnumSet.of(Field.HEADERS), blacklist);
        return owner;
    }

    /**
     * Logs only the status code and status message at INFO level.
     *
     * @return the owning parent
     */
    public P status() {
        ResponseLogger.log(raw, EnumSet.of(Field.STATUS), blacklist);
        return owner;
    }

    /**
     * Logs the full response at ERROR level if the status code is &ge; 400.
     * Does nothing when the response is successful (status &lt; 400).
     *
     * @return the owning parent
     */
    public P ifError() {
        if (raw.statusCode() >= 400) {
            ResponseLogger.logError(raw, blacklist);
        }
        return owner;
    }

    /**
     * Logs the full response at ERROR level if the status code equals the given value.
     *
     * @param statusCode the status code to match
     * @return the owning parent
     */
    public P ifStatusCodeIsEqualTo(int statusCode) {
        if (raw.statusCode() == statusCode) {
            ResponseLogger.logError(raw, blacklist);
        }
        return owner;
    }

    /**
     * Logs the full response at ERROR level if the status code matches the given predicate.
     *
     * @param statusCode the status code to check against
     * @return the owning parent
     */
    public P ifStatusCodeMatches(java.util.function.IntPredicate statusCode) {
        if (statusCode.test(raw.statusCode())) {
            ResponseLogger.logError(raw, blacklist);
        }
        return owner;
    }

    /**
     * Activates deferred logging: the full response will be emitted at ERROR level
     * only if a subsequent assertion on the owning {@code ValidatableResponse} fails.
     * <p>
     * This method is only meaningful when the parent is a {@code ValidatableResponse}.
     * Calling it on a plain {@code Response}-backed spec has no effect.
     * </p>
     *
     * @return the owning parent
     */
    public P ifValidationFails() {
        if (onFailureActivator != null) {
            onFailureActivator.activate(blacklist);
        }
        return owner;
    }
}
