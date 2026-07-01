package io.smallrye.httpassured;

import io.smallrye.httpassured.config.HttpAssuredConfig;
import io.smallrye.httpassured.dsl.RequestBuilder;
import io.smallrye.httpassured.spi.HttpClientEngine;
import io.smallrye.httpassured.spi.ObjectMapperProvider;
import io.smallrye.httpassured.websocket.WsSessionBuilder;

import java.time.Duration;

/**
 * Main entry point for http-assured.
 * <p>
 * Create an instance via {@link #builder()} and use the fluent DSL:
 * </p>
 * <pre>{@code
 * HttpAssured client = HttpAssured.builder()
 *     .baseUri("http://localhost:8080")
 *     .defaultHeader("Accept", "application/json")
 *     .build();
 *
 * client.given()
 *     .header("Authorization", "Bearer token")
 *     .queryParam("page", "1")
 * .when()
 *     .get("/users")
 * .then()
 *     .statusCode(200)
 *     .body("users[0].name", isEqualTo("John"));
 * }</pre>
 * <p>
 * Instances are thread-safe and reusable. Each instance holds its own configuration
 * (no static mutable state).
 * </p>
 */
public final class HttpAssured implements AutoCloseable {

    private final HttpAssuredConfig config;

    HttpAssured(HttpAssuredConfig config) {
        this.config = config;
    }

    /**
     * Starts building a request with configuration (the "given" phase).
     *
     * @return a request builder
     */
    public RequestBuilder given() {
        return new RequestBuilder(config);
    }

    /**
     * Shortcut: starts a request and immediately transitions to the "when" phase.
     * Equivalent to {@code given().when()}.
     *
     * @return a request builder in the "when" phase
     */
    public RequestBuilder when() {
        return given();
    }

    /**
     * Starts building a WebSocket connection.
     *
     * @param path the WebSocket endpoint path
     * @return a WebSocket session builder
     */
    public WsSessionBuilder webSocket(String path) {
        return new WsSessionBuilder(config, config.engine(), path);
    }

    /**
     * Returns the underlying configuration.
     */
    public HttpAssuredConfig config() {
        return config;
    }

    /**
     * Returns the HTTP client engine.
     */
    public HttpClientEngine engine() {
        return config.engine();
    }

    /**
     * Creates a new instance with a different engine but preserves all other configuration.
     * <p>
     * Useful for wrapping the engine (e.g., with ToxicEngine) without losing other config.
     *
     * @param engine the new engine to use
     * @return a new HttpAssured instance with the same settings but a different engine
     */
    public HttpAssured withEngine(HttpClientEngine engine) {
        return new HttpAssured(config.withEngine(engine));
    }

    /**
     * Closes the client and releases resources.
     */
    @Override
    public void close() {
        config.engine().close();
    }

    /**
     * Creates a new builder for configuring an HttpAssured instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final HttpAssuredConfig.Builder configBuilder = HttpAssuredConfig.builder();

        private Builder() {}

        /**
         * Sets the base URI for all requests.
         */
        public Builder baseUri(String baseUri) {
            configBuilder.baseUri(baseUri);
            return this;
        }

        /**
         * Sets the port for all requests.
         */
        public Builder port(int port) {
            configBuilder.port(port);
            return this;
        }

        /**
         * Sets the base path appended after the base URI.
         */
        public Builder basePath(String basePath) {
            configBuilder.basePath(basePath);
            return this;
        }

        /**
         * Adds a default header to all requests.
         */
        public Builder defaultHeader(String name, String value) {
            configBuilder.defaultHeader(name, value);
            return this;
        }

        /**
         * Sets the HTTP client engine.
         */
        public Builder engine(HttpClientEngine engine) {
            configBuilder.engine(engine);
            return this;
        }

        /**
         * Sets the object mapper for serialization/deserialization.
         */
        public Builder objectMapper(ObjectMapperProvider objectMapper) {
            configBuilder.objectMapper(objectMapper);
            return this;
        }

        /**
         * Sets the default request timeout.
         */
        public Builder requestTimeout(Duration timeout) {
            configBuilder.requestTimeout(timeout);
            return this;
        }

        /**
         * Adds a header name to the log blacklist. Its value will be replaced
         * with {@code [ BLACKLISTED ]} in log output. The default blacklist already
         * contains {@code Authorization} and {@code Cookie}.
         */
        public Builder blacklistHeader(String name) {
            configBuilder.blacklistHeader(name);
            return this;
        }

        /**
         * Adds multiple header names to the log blacklist.
         */
        public Builder blacklistHeaders(String... names) {
            configBuilder.blacklistHeaders(names);
            return this;
        }

        /**
         * Removes all entries from the log blacklist, including the defaults
         * ({@code Authorization} and {@code Cookie}).
         */
        public Builder clearHeaderBlacklist() {
            configBuilder.clearHeaderBlacklist();
            return this;
        }

        /**
         * Enables automatic request logging when a validation fails, for every
         * request made by this instance.
         */
        public Builder logRequestIfValidationFails() {
            configBuilder.logRequestIfValidationFails(true);
            return this;
        }

        /**
         * Enables automatic response logging when a validation fails, for every
         * response received by this instance.
         */
        public Builder logResponseIfValidationFails() {
            configBuilder.logResponseIfValidationFails(true);
            return this;
        }

        /**
         * Convenience method: enables both request and response logging on validation failure.
         * Equivalent to calling {@link #logRequestIfValidationFails()} and
         * {@link #logResponseIfValidationFails()}.
         */
        public Builder logIfValidationFails() {
            configBuilder.logRequestIfValidationFails(true);
            configBuilder.logResponseIfValidationFails(true);
            return this;
        }

        /**
         * Builds the HttpAssured instance.
         */
        public HttpAssured build() {
            return new HttpAssured(configBuilder.build());
        }
    }
}
