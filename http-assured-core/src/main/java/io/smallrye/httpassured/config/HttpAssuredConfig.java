package io.smallrye.httpassured.config;

import io.smallrye.httpassured.engine.vertx.VertxHttpEngine;
import io.smallrye.httpassured.http.Headers;
import io.smallrye.httpassured.internal.UriSupport;
import io.smallrye.httpassured.mapper.jackson.JacksonObjectMapperProvider;
import io.smallrye.httpassured.spi.HttpClientEngine;
import io.smallrye.httpassured.spi.ObjectMapperProvider;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable configuration for an {@link io.smallrye.httpassured.HttpAssured} instance.
 * <p>
 * Configuration is instance-based — no static mutable state.
 * </p>
 */
public final class HttpAssuredConfig {

    /** Default set of header names whose values are masked in log output. */
    static final Set<String> DEFAULT_BLACKLISTED_HEADERS = Set.of("Authorization", "Cookie");

    private final String baseUri;
    private final int port;
    private final String basePath;
    private final Headers defaultHeaders;
    private final HttpClientEngine engine;
    private final ObjectMapperProvider objectMapper;
    private final Duration requestTimeout;
    private final Set<String> blacklistedHeaders;
    private final boolean logRequestIfValidationFails;
    private final boolean logResponseIfValidationFails;

    private HttpAssuredConfig(Builder builder) {
        this.baseUri = builder.baseUri;
        this.port = builder.port;
        this.basePath = builder.basePath;
        this.defaultHeaders = builder.defaultHeaders;
        this.engine = builder.engine != null ? builder.engine : new VertxHttpEngine();
        this.objectMapper = builder.objectMapper != null ? builder.objectMapper : new JacksonObjectMapperProvider();
        this.requestTimeout = builder.requestTimeout;
        TreeSet<String> copy = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        copy.addAll(builder.blacklistedHeaders);
        this.blacklistedHeaders = Collections.unmodifiableSet(copy);
        this.logRequestIfValidationFails = builder.logRequestIfValidationFails;
        this.logResponseIfValidationFails = builder.logResponseIfValidationFails;
    }

    public String baseUri() {
        return baseUri;
    }

    public int port() {
        return port;
    }

    public String basePath() {
        return basePath;
    }

    public Headers defaultHeaders() {
        return defaultHeaders;
    }

    public HttpClientEngine engine() {
        return engine;
    }

    public ObjectMapperProvider objectMapper() {
        return objectMapper;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    /**
     * Returns the set of header names whose values are replaced with
     * {@code [ BLACKLISTED ]} in log output. The set uses case-insensitive
     * comparison at lookup time in the loggers.
     */
    public Set<String> blacklistedHeaders() {
        return blacklistedHeaders;
    }

    /**
     * Returns {@code true} if every request from this instance should be logged
     * at ERROR level when a validation fails.
     */
    public boolean logRequestIfValidationFails() {
        return logRequestIfValidationFails;
    }

    /**
     * Returns {@code true} if every response from this instance should be logged
     * at ERROR level when a validation fails.
     */
    public boolean logResponseIfValidationFails() {
        return logResponseIfValidationFails;
    }

    /**
     * Creates a new configuration with a different engine but preserves all other settings.
     * <p>
     * Useful for wrapping the engine (e.g., with ToxicEngine) without losing other config.
     *
     * @param engine the new engine to use
     * @return a new configuration with the same settings but a different engine
     */
    public HttpAssuredConfig withEngine(HttpClientEngine engine) {
        Builder b = new Builder();
        b.baseUri = this.baseUri;
        b.port = this.port;
        b.basePath = this.basePath;
        b.defaultHeaders = this.defaultHeaders;
        b.engine = engine;
        b.objectMapper = this.objectMapper;
        b.requestTimeout = this.requestTimeout;
        b.blacklistedHeaders.clear();
        b.blacklistedHeaders.addAll(this.blacklistedHeaders);
        b.logRequestIfValidationFails = this.logRequestIfValidationFails;
        b.logResponseIfValidationFails = this.logResponseIfValidationFails;
        return b.build();
    }

    /**
     * Resolves a request path against the base URI, port, and base path.
     */
    public String resolveUri(String path) {
        StringBuilder sb = new StringBuilder();
        if (baseUri != null && !baseUri.isEmpty()) {
            sb.append(baseUri);
        }
        if (port > 0 && baseUri != null && !baseUri.isEmpty()) {
            String withPort = UriSupport.injectPort(sb.toString(), port);
            sb = new StringBuilder(withPort);
        }
        if (basePath != null && !basePath.isEmpty()) {
            if (!basePath.startsWith("/")) {
                sb.append("/");
            }
            sb.append(basePath);
        }
        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/") && (sb.isEmpty() || sb.charAt(sb.length() - 1) != '/')) {
                sb.append("/");
            }
            sb.append(path);
        }
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String baseUri = "";
        private int port = -1;
        private String basePath = "";
        private Headers defaultHeaders = new Headers();
        private HttpClientEngine engine;
        private ObjectMapperProvider objectMapper;
        private Duration requestTimeout = Duration.ofSeconds(30);
        // Case-insensitive via TreeSet with CASE_INSENSITIVE_ORDER
        private final Set<String> blacklistedHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        private boolean logRequestIfValidationFails = false;
        private boolean logResponseIfValidationFails = false;

        private Builder() {
            blacklistedHeaders.addAll(DEFAULT_BLACKLISTED_HEADERS);
        }

        public Builder baseUri(String baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public Builder defaultHeader(String name, String value) {
            this.defaultHeaders = this.defaultHeaders.with(name, value);
            return this;
        }

        public Builder defaultHeaders(Headers headers) {
            this.defaultHeaders = headers;
            return this;
        }

        public Builder engine(HttpClientEngine engine) {
            this.engine = Objects.requireNonNull(engine);
            return this;
        }

        public Builder objectMapper(ObjectMapperProvider objectMapper) {
            this.objectMapper = Objects.requireNonNull(objectMapper);
            return this;
        }

        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = Objects.requireNonNull(timeout);
            return this;
        }

        /**
         * Adds a header name to the blacklist. Its value will be replaced with
         * {@code [ BLACKLISTED ]} in log output.
         */
        public Builder blacklistHeader(String name) {
            this.blacklistedHeaders.add(Objects.requireNonNull(name));
            return this;
        }

        /**
         * Adds multiple header names to the blacklist.
         */
        public Builder blacklistHeaders(String... names) {
            for (String name : names) {
                blacklistedHeaders.add(Objects.requireNonNull(name));
            }
            return this;
        }

        /**
         * Removes all entries from the blacklist, including the defaults
         * ({@code Authorization} and {@code Cookie}). Use this when you want
         * no masking at all, or to replace the defaults entirely.
         */
        public Builder clearHeaderBlacklist() {
            this.blacklistedHeaders.clear();
            return this;
        }

        public Builder logRequestIfValidationFails(boolean value) {
            this.logRequestIfValidationFails = value;
            return this;
        }

        public Builder logResponseIfValidationFails(boolean value) {
            this.logResponseIfValidationFails = value;
            return this;
        }

        public HttpAssuredConfig build() {
            return new HttpAssuredConfig(this);
        }
    }
}
