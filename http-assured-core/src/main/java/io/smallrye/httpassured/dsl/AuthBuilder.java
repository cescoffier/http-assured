package io.smallrye.httpassured.dsl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Fluent sub-builder for request authentication.
 *
 * <p>Returned by {@link RequestBuilder#auth()}. Mirrors the authentication API from
 * <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#authentication">REST Assured</a>.
 *
 * <h2>Supported mechanisms</h2>
 *
 * <h3>Basic auth</h3>
 * <pre>{@code
 * client.given()
 *     .auth().basic("user", "secret")
 *     .when().get("/secure");
 * }</pre>
 *
 * <h3>Pre-emptive Basic auth</h3>
 * <pre>{@code
 * client.given()
 *     .auth().preemptive().basic("user", "secret")
 *     .when().get("/secure");
 * }</pre>
 * <p>In http-assured, {@code basic()} and {@code preemptive().basic()} are functionally identical:
 * both unconditionally set the {@code Authorization: Basic} header. Challenged authentication
 * (sending the request unauthenticated and re-sending after a {@code 401} response) is not
 * supported because the engine SPI has no interceptor hook for a challenge round-trip.
 *
 * <h3>OAuth 2 bearer token</h3>
 * <pre>{@code
 * client.given()
 *     .auth().oauth2("my-access-token")
 *     .when().get("/api/resource");
 * }</pre>
 *
 * <h3>OAuth 1 HMAC-SHA1</h3>
 * <pre>{@code
 * client.given()
 *     .auth().oauth("consumerKey", "consumerSecret", "accessToken", "tokenSecret")
 *     .when().get("/api/resource");
 * }</pre>
 * <p>OAuth 1 signing is deferred: the {@code Authorization} header is computed inside
 * {@code execute()} once the full request URI (base URI + path + path/query params) is known.
 */
public final class AuthBuilder {

    private final RequestBuilder parent;

    AuthBuilder(RequestBuilder parent) {
        this.parent = parent;
    }

    /**
     * Authenticates using HTTP Basic auth.
     *
     * <p>Sets {@code Authorization: Basic <base64(user:password)>} on the request.
     *
     * <p><b>Note:</b> Unlike REST Assured, this does not perform a 401-challenge round-trip.
     * Credentials are always sent pre-emptively. Use
     * {@link #preemptive()}{@code .basic(user, password)} for an API that makes the intent explicit.
     *
     * @param user     the username
     * @param password the password
     * @return the parent {@link RequestBuilder} to continue the fluent chain
     */
    public RequestBuilder basic(String user, String password) {
        return parent.header("Authorization", "Basic " + encodeBasic(user, password));
    }

    /**
     * Encodes username and password as a Base64-encoded Basic auth credential string.
     *
     * @param user     the username
     * @param password the password
     * @return the Base64-encoded string {@code base64(user:password)}
     */
    static String encodeBasic(String user, String password) {
        String credentials = user + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns a {@link PreemptiveAuthBuilder} for configuring pre-emptive authentication.
     *
     * <p>Example:
     * <pre>{@code
     * client.given()
     *     .auth().preemptive().basic("user", "secret")
     *     .when().get("/secure");
     * }</pre>
     *
     * @return a preemptive auth sub-builder
     */
    public PreemptiveAuthBuilder preemptive() {
        return new PreemptiveAuthBuilder(parent);
    }

    /**
     * Authenticates using an OAuth 2 bearer token.
     *
     * <p>Sets {@code Authorization: Bearer <accessToken>} on the request.
     *
     * @param accessToken the OAuth 2 access token
     * @return the parent {@link RequestBuilder} to continue the fluent chain
     */
    public RequestBuilder oauth2(String accessToken) {
        return parent.header("Authorization", "Bearer " + accessToken);
    }

    /**
     * Authenticates using OAuth 1 HMAC-SHA1 request signing.
     *
     * <p>Stores the OAuth 1 credentials on the request builder. The {@code Authorization} header
     * is computed and written inside {@code execute()} once the full request URI is known,
     * ensuring the HMAC-SHA1 signature covers the correct URL (including any path and query
     * parameters set after this call).
     *
     * @param consumerKey    the OAuth 1 consumer (application) key
     * @param consumerSecret the OAuth 1 consumer (application) secret
     * @param accessToken    the OAuth 1 access token
     * @param tokenSecret    the OAuth 1 access token secret
     * @return the parent {@link RequestBuilder} to continue the fluent chain
     */
    public RequestBuilder oauth(String consumerKey, String consumerSecret,
                                String accessToken, String tokenSecret) {
        parent.setOAuth1Credentials(new RequestBuilder.OAuth1Credentials(
                consumerKey, consumerSecret, accessToken, tokenSecret));
        return parent;
    }
}
