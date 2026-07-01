package io.smallrye.httpassured.dsl;

/**
 * Fluent sub-builder for configuring HTTP redirect behavior on a per-request basis.
 *
 * <p>Returned by {@link RequestBuilder#redirects()}. Mirrors the redirect API from
 * <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#redirects">REST Assured</a>.
 *
 * <h2>Disable redirect following</h2>
 * <pre>{@code
 * client.given()
 *     .redirects().follow(false)
 *     .when().get("/redirect")
 *     .then()
 *     .statusCode(302)
 *     .header("Location", isEqualTo("/target"));
 * }</pre>
 *
 * <h2>Limit maximum redirects</h2>
 * <pre>{@code
 * client.given()
 *     .redirects().max(3)
 *     .when().get("/chain-start")
 *     .then()
 *     .statusCode(200);
 * }</pre>
 *
 * <p><b>Differences from REST Assured:</b>
 * <ul>
 *   <li>http-assured supports redirect config per-request only; REST Assured also supports global config</li>
 *   <li>{@code allowCircular(true)} is not supported</li>
 * </ul>
 */
public final class RedirectSpec {

    private final RequestBuilder parent;

    RedirectSpec(RequestBuilder parent) {
        this.parent = parent;
    }

    /**
     * Controls whether the HTTP client should automatically follow redirect responses
     * (3xx with a {@code Location} header).
     *
     * <p>When {@code false}, the raw redirect response (status code 301, 302, etc.) is returned
     * directly, allowing assertions on the redirect itself.
     *
     * @param follow {@code true} to follow redirects (the default), {@code false} to return
     *               the redirect response as-is
     * @return the parent {@link RequestBuilder} to continue the fluent chain
     */
    public RequestBuilder follow(boolean follow) {
        parent.setFollowRedirects(follow);
        return parent;
    }

    /**
     * Sets the maximum number of redirects to follow before stopping.
     *
     * <p>If the redirect chain exceeds this limit, the behavior depends on the underlying
     * HTTP engine. With Vert.x, the last redirect response is returned.
     *
     * <p>Setting {@code max} implicitly enables redirect following.
     *
     * @param maxRedirects the maximum number of redirects to follow; must be positive
     * @return the parent {@link RequestBuilder} to continue the fluent chain
     * @throws IllegalArgumentException if {@code maxRedirects} is not positive
     */
    public RequestBuilder max(int maxRedirects) {
        if (maxRedirects < 1) {
            throw new IllegalArgumentException("maxRedirects must be positive, got: " + maxRedirects);
        }
        parent.setMaxRedirects(maxRedirects);
        return parent;
    }
}
