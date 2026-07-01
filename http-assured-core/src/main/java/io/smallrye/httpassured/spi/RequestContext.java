package io.smallrye.httpassured.spi;

import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.http.Headers;
import io.smallrye.httpassured.http.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable representation of an HTTP request to be executed by an {@link HttpClientEngine}.
 * Built by the DSL layer and passed to the engine.
 */
public final class RequestContext {

    private final HttpMethod method;
    private final String uri;
    private final Headers headers;
    private final Map<String, List<String>> queryParams;
    private final Map<String, String> pathParams;
    private final byte[] body;
    private final ContentType contentType;
    private final Map<String, Object> attributes;
    private final boolean trustAll;
    private final TrustOptions trustOptions;

    private RequestContext(Builder builder) {
        this.method = builder.method;
        this.uri = builder.uri;
        this.headers = builder.headers;
        Map<String, List<String>> snapshot = new LinkedHashMap<>();
        builder.queryParams.forEach((k, v) -> snapshot.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
        this.queryParams = Collections.unmodifiableMap(snapshot);
        this.pathParams = Collections.unmodifiableMap(builder.pathParams);
        this.body = builder.body;
        this.contentType = builder.contentType;
        this.attributes = Collections.unmodifiableMap(builder.attributes);
        this.trustAll = builder.trustAll;
        this.trustOptions = builder.trustOptions;
    }

    public HttpMethod method() {
        return method;
    }

    public String uri() {
        return uri;
    }

    public Headers headers() {
        return headers;
    }

    public Map<String, List<String>> queryParams() {
        return queryParams;
    }

    public Map<String, String> pathParams() {
        return pathParams;
    }

    public Optional<byte[]> body() {
        return Optional.ofNullable(body);
    }

    public Optional<ContentType> contentType() {
        return Optional.ofNullable(contentType);
    }

    public boolean trustAll() {
        return trustAll;
    }

    public Optional<TrustOptions> trustOptions() {
        return Optional.ofNullable(trustOptions);
    }

    /**
     * Custom attributes for engine-specific configuration.
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private HttpMethod method = HttpMethod.GET;
        private String uri = "/";
        private Headers headers = new Headers();
        private final Map<String, List<String>> queryParams = new LinkedHashMap<>();
        private final Map<String, String> pathParams = new LinkedHashMap<>();
        private byte[] body;
        private ContentType contentType;
        private boolean trustAll = false;
        private TrustOptions trustOptions;
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        private Builder() {}

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder trustAll(boolean trustAll) {
            this.trustAll = trustAll;
            return this;
        }

        public Builder trustOptions(TrustOptions trustOptions) {
            this.trustOptions = trustOptions;
            return this;
        }

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
            this.queryParams.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            return this;
        }

        public Builder queryParam(String name, List<String> values) {
            this.queryParams.computeIfAbsent(name, k -> new ArrayList<>()).addAll(values);
            return this;
        }

        public Builder queryParams(Map<String, List<String>> params) {
            params.forEach((name, values) ->
                    this.queryParams.computeIfAbsent(name, k -> new ArrayList<>()).addAll(values));
            return this;
        }

        public Builder pathParam(String name, String value) {
            this.pathParams.put(name, value);
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public Builder contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public RequestContext build() {
            return new RequestContext(this);
        }
    }
}
