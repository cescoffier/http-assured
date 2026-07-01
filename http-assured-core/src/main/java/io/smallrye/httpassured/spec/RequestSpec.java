package io.smallrye.httpassured.spec;

import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.http.Headers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A reusable request specification.
 * <p>
 * Captures request configuration (headers, params, body, content type) that can
 * be applied to multiple requests via {@code given().spec(requestSpec)}.
 * </p>
 * <pre>{@code
 * RequestSpec authSpec = RequestSpec.builder()
 *     .header("Authorization", "Bearer token")
 *     .contentType(ContentType.JSON)
 *     .build();
 *
 * client.given().spec(authSpec).when().get("/users")...
 * }</pre>
 */
public final class RequestSpec {

    private final Headers headers;
    private final Map<String, List<String>> queryParams;
    private final Map<String, String> pathParams;
    private final Map<String, String> formParams;
    private final ContentType contentType;
    private final byte[] body;

    private RequestSpec(Builder builder) {
        this.headers = builder.headers;
        Map<String, List<String>> snapshot = new LinkedHashMap<>();
        builder.queryParams.forEach((k, v) -> snapshot.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
        this.queryParams = Collections.unmodifiableMap(snapshot);
        this.pathParams = Collections.unmodifiableMap(builder.pathParams);
        this.formParams = Collections.unmodifiableMap(builder.formParams);
        this.contentType = builder.contentType;
        this.body = builder.body;
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

    public Map<String, String> formParams() {
        return formParams;
    }

    public ContentType contentType() {
        return contentType;
    }

    public byte[] body() {
        return body;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Headers headers = new Headers();
        private final Map<String, List<String>> queryParams = new LinkedHashMap<>();
        private final Map<String, String> pathParams = new LinkedHashMap<>();
        private final Map<String, String> formParams = new LinkedHashMap<>();
        private ContentType contentType;
        private byte[] body;

        private Builder() {}

        public Builder header(String name, String value) {
            this.headers = this.headers.with(name, value);
            return this;
        }

        public Builder headers(Headers headers) {
            this.headers = headers;
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

        public Builder pathParam(String name, String value) {
            this.pathParams.put(name, value);
            return this;
        }

        public Builder formParam(String name, String value) {
            this.formParams.put(name, value);
            return this;
        }

        public Builder accept(String contentType) {
            this.headers = this.headers.replacing("Accept", contentType);
            return this;
        }

        public Builder accept(ContentType contentType) {
            return accept(contentType.value());
        }

        public Builder contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public RequestSpec build() {
            return new RequestSpec(this);
        }
    }
}
