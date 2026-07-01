package io.smallrye.httpassured.log;

import io.smallrye.httpassured.dsl.RequestBuilder;

import java.util.EnumSet;
import java.util.Set;

/**
 * Fluent specification for request-side logging.
 * <p>
 * Obtained via {@link RequestBuilder#log()}. Each method selects what to log
 * and returns the owning {@link RequestBuilder} so the DSL chain can continue:
 * </p>
 * <pre>{@code
 * client.given()
 *     .log().all()
 *     .when().get("/users")
 *     .then().statusCode(200);
 * }</pre>
 * <p>
 * Logging is performed at INFO level via JBoss Logging immediately before the
 * request is sent to the engine. Headers in the configured blacklist are printed
 * as {@code [ BLACKLISTED ]}.
 * </p>
 */
public final class RequestLogSpec {

    /**
     * Fields that can appear in a request log entry.
     */
    enum Field {
        METHOD, URI, HEADERS, PARAMS, BODY
    }

    private final RequestBuilder owner;
    private final EnumSet<Field> fields = EnumSet.noneOf(Field.class);
    private final Set<String> blacklist;

    /**
     * Created only by {@link RequestBuilder#log()}.
     *
     * @param owner     the owning request builder
     * @param blacklist case-insensitive set of header names to mask
     */
    public RequestLogSpec(RequestBuilder owner, Set<String> blacklist) {
        this.owner = owner;
        this.blacklist = blacklist;
    }

    /**
     * Logs the HTTP method, URI, all headers, and the body.
     *
     * @return the owning {@link RequestBuilder}
     */
    public RequestBuilder all() {
        fields.addAll(EnumSet.allOf(Field.class));
        return owner;
    }

    /**
     * Logs only the request body.
     *
     * @return the owning {@link RequestBuilder}
     */
    public RequestBuilder body() {
        fields.add(Field.BODY);
        return owner;
    }

    /**
     * Logs only the request headers (sensitive values are masked).
     *
     * @return the owning {@link RequestBuilder}
     */
    public RequestBuilder headers() {
        fields.add(Field.HEADERS);
        return owner;
    }

    /**
     * Logs query parameters and named path parameters.
     *
     * @return the owning {@link RequestBuilder}
     */
    public RequestBuilder params() {
        fields.add(Field.PARAMS);
        return owner;
    }

    /**
     * Logs only the HTTP method.
     *
     * @return the owning {@link RequestBuilder}
     */
    public RequestBuilder method() {
        fields.add(Field.METHOD);
        return owner;
    }

    /**
     * Logs only the resolved URI.
     *
     * @return the owning {@link RequestBuilder}
     */
    public RequestBuilder uri() {
        fields.add(Field.URI);
        return owner;
    }

    /**
     * Returns true if at least one log field has been selected.
     */
    public boolean hasFields() {
        return !fields.isEmpty();
    }

    public EnumSet<Field> fields() {
        return fields;
    }

    public Set<String> blacklist() {
        return blacklist;
    }
}
