package io.smallrye.httpassured.http;

import java.util.Objects;

/**
 * An HTTP cookie.
 *
 * <p>Instances are immutable and constructed via the nested {@link Builder}:
 * <pre>{@code
 * Cookie c = Cookie.builder("session_id", "1234")
 *     .secure(true)
 *     .domain("example.com")
 *     .build();
 * }</pre>
 */
public final class Cookie {

    private final String name;
    private final String value;
    private final boolean secured;
    private final String comment;
    private final String domain;
    private final String path;
    private final long maxAge;
    private final boolean httpOnly;
    private final String sameSite;

    private Cookie(Builder builder) {
        this.name = builder.name;
        this.value = builder.value;
        this.secured = builder.secured;
        this.comment = builder.comment;
        this.domain = builder.domain;
        this.path = builder.path;
        this.maxAge = builder.maxAge;
        this.httpOnly = builder.httpOnly;
        this.sameSite = builder.sameSite;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public boolean isSecured() {
        return secured;
    }

    public String comment() {
        return comment;
    }

    public String domain() {
        return domain;
    }

    public String path() {
        return path;
    }

    public long maxAge() {
        return maxAge;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public String sameSite() {
        return sameSite;
    }

    public String toHeaderValue() {
        return name + "=" + value;
    }

    @Override
    public String toString() {
        return "Cookie{name='" + name + "', value='" + value + "'}";
    }

    public static Builder builder(String name, String value) {
        return new Builder(name, value);
    }

    public static final class Builder {

        private final String name;
        private final String value;
        private boolean secured = false;
        private String comment = null;
        private String domain;
        private String path;
        private long maxAge = -1;
        private boolean httpOnly = false;
        private String sameSite;

        public Builder(String name, String value) {
            this.name = Objects.requireNonNull(name, "Cookie name must not be null");
            this.value = Objects.requireNonNull(value, "Cookie value must not be null");
        }

        public Builder secure(boolean secure) {
            this.secured = secure;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder maxAge(long maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder httpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        public Builder sameSite(String sameSite) {
            this.sameSite = sameSite;
            return this;
        }

        public Cookie build() {
            return new Cookie(this);
        }
    }
}
