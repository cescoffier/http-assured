package io.smallrye.httpassured.dsl;

import io.smallrye.httpassured.config.HttpAssuredConfig;
import io.smallrye.httpassured.http.Cookie;
import io.smallrye.httpassured.http.Headers;
import io.smallrye.httpassured.http.HttpVersion;
import io.smallrye.httpassured.internal.SetCookieParser;
import io.smallrye.httpassured.log.ResponseLogSpec;
import io.smallrye.httpassured.spi.RawResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

/**
 * Represents an HTTP response.
 * <p>
 * Provides access to response data and a transition to validation via {@link #then()}.
 * </p>
 */
public final class Response {

    private static final ObjectMapper PRETTY_MAPPER = new ObjectMapper();

    private final RawResponse raw;
    private final HttpAssuredConfig config;

    public Response(RawResponse raw, HttpAssuredConfig config) {
        this.raw = raw;
        this.config = config;
    }

    /**
     * Transitions to the validation phase.
     */
    public ValidatableResponse then() {
        return new ValidatableResponse(raw, config);
    }

    /**
     * Returns a {@link ResponseLogSpec} to log details about this response.
     * <p>
     * Supports {@code all()}, {@code body()}, {@code headers()}, {@code status()},
     * and {@code ifError()}. The {@code ifValidationFails()} variant is not available
     * on {@code Response} — use {@code .then().log().ifValidationFails()} instead.
     * </p>
     * <pre>{@code
     * Response response = client.when().get("/users");
     * response.log().all();
     * response.then().statusCode(200);
     * }</pre>
     */
    public ResponseLogSpec<Response> log() {
        return new ResponseLogSpec<>(this, raw, config.blacklistedHeaders(), null);
    }

    /**
     * Returns the HTTP status code.
     */
    public int statusCode() {
        return raw.statusCode();
    }

    /**
     * Returns the response headers.
     */
    public Headers headers() {
        return raw.headers();
    }

    /**
     * Returns the value of the first header with the given name, or {@code null} if no such
     * header is present.
     *
     * <p>The lookup is case-insensitive. This method is provided for REST Assured API parity;
     * prefer {@link #headers()}{@code .getValue(name)} when an {@link java.util.Optional} return
     * type is more appropriate.
     *
     * @param name the header name; must not be {@code null}
     * @return the header value, or {@code null} if absent
     */
    public String getHeader(String name) {
        return raw.headers().getValue(name).orElse(null);
    }

    /**
     * Returns the raw response body as bytes.
     */
    public byte[] bodyAsBytes() {
        return raw.body();
    }

    /**
     * Returns the response body as a UTF-8 string.
     */
    public String bodyAsString() {
        return raw.bodyAsString();
    }

    /**
     * Deserializes the response body to the given type using the configured object mapper.
     */
    public <T> T bodyAs(Class<T> type) {
        return config.objectMapper().deserialize(raw.body(), type);
    }

    public <T> T bodyAs(TypeReference<T> type) {
        return (T) config.objectMapper().deserialize(raw.body(), type.getType());
    }

    /**
     * Returns the response time in milliseconds.
     */
    public long time() {
        return raw.responseTimeMs();
    }

    /**
     * Returns the response time in the specified time unit.
     */
    public long timeIn(TimeUnit timeUnit) {
        return timeUnit.convert(raw.responseTimeMs(), TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the response body as a pretty-printed JSON string.
     * Falls back to the raw body string if the body is not valid JSON.
     */
    public String asPrettyString() {
        return prettyFormat(raw.bodyAsString());
    }

    /**
     * Pretty-prints the response body to stdout and returns this response.
     */
    public Response prettyPrint() {
        System.out.println(asPrettyString());
        return this;
    }

    /**
     * Prints the response body to stdout and returns this response.
     */
    public Response peek() {
        System.out.println(raw.bodyAsString());
        return this;
    }

    /**
     * Pretty-prints the response body to stdout and returns this response.
     */
    public Response prettyPeek() {
        return prettyPrint();
    }

    /**
     * Returns the HTTP protocol version used for the response.
     */
    public HttpVersion httpVersion() {
        return raw.httpVersion();
    }

    /**
     * Returns the HTTP status line (e.g. "200 OK").
     */
    public String statusLine() {
        return raw.statusCode() + " " + raw.statusMessage();
    }

    /**
     * Returns a parsed cookie from the {@code Set-Cookie} response headers.
     *
     * @param name the cookie name
     * @return the cookie, or {@code null} if not present
     */
    public Cookie detailedCookie(String name) {
        return SetCookieParser.findCookie(raw.headers().getValues("Set-Cookie"), name);
    }

    /**
     * Returns the underlying raw response.
     */
    public RawResponse rawResponse() {
        return raw;
    }

    private static String prettyFormat(String json) {
        if (json == null || json.isBlank()) return json;
        try {
            Object obj = PRETTY_MAPPER.readValue(json, Object.class);
            return PRETTY_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return json;
        }
    }
}
