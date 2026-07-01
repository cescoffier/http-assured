package io.smallrye.httpassured.spi;

import io.smallrye.httpassured.http.Headers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for opening a WebSocket connection.
 * Built by the WebSocket DSL and passed to the engine.
 */
public final class WebSocketContext {

    private final String uri;
    private final Headers headers;
    private final Map<String, String> queryParams;
    private final Map<String, Object> attributes;

    private WebSocketContext(Builder builder) {
        this.uri = builder.uri;
        this.headers = builder.headers;
        this.queryParams = Collections.unmodifiableMap(builder.queryParams);
        this.attributes = Collections.unmodifiableMap(builder.attributes);
    }

    public String uri() {
        return uri;
    }

    public Headers headers() {
        return headers;
    }

    public Map<String, String> queryParams() {
        return queryParams;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String uri = "/";
        private Headers headers = new Headers();
        private final Map<String, String> queryParams = new LinkedHashMap<>();
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        private Builder() {}

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder headers(Headers headers) {
            this.headers = headers;
            return this;
        }

        public Builder addHeader(String name, String value) {
            this.headers = this.headers.with(name, value);
            return this;
        }

        public Builder queryParam(String name, String value) {
            this.queryParams.put(name, value);
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public WebSocketContext build() {
            return new WebSocketContext(this);
        }
    }
}
