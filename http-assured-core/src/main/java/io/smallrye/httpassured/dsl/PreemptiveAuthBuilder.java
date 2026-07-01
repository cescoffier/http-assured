package io.smallrye.httpassured.dsl;

/**
 * Fluent sub-builder for pre-emptive authentication.
 *
 * <p>Returned by {@link AuthBuilder#preemptive()}. Pre-emptive authentication unconditionally
 * sends credentials on every request without waiting for a {@code 401} challenge.
 *
 * <p>Example:
 * <pre>{@code
 * client.given()
 *     .auth().preemptive().basic("user", "secret")
 *     .when().get("/secure");
 * }</pre>
 */
public final class PreemptiveAuthBuilder {

    private final RequestBuilder parent;

    PreemptiveAuthBuilder(RequestBuilder parent) {
        this.parent = parent;
    }

    /**
     * Sends pre-emptive HTTP Basic authentication by unconditionally setting
     * {@code Authorization: Basic <base64(user:password)>} on every request.
     *
     * <p>In http-assured this is functionally identical to {@link AuthBuilder#basic(String, String)}
     * because the framework does not perform 401-challenge round-trips.
     *
     * @param user     the username
     * @param password the password
     * @return the parent {@link RequestBuilder} to continue the fluent chain
     */
    public RequestBuilder basic(String user, String password) {
        return parent.header("Authorization", "Basic " + AuthBuilder.encodeBasic(user, password));
    }
}
