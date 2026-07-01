package io.smallrye.httpassured.spi;

import java.nio.file.Path;

/**
 * Configures client-side TLS trust verification.
 * <p>
 * Use one of the static factory methods to create an instance and pass it to
 * {@code RequestBuilder.trustOptions(TrustOptions)} to have the HTTP client validate the
 * server's certificate against a specific trust store rather than bypassing validation entirely
 * with {@code trustAll(true)}.
 * </p>
 *
 * <pre>{@code
 * client.given()
 *     .trustOptions(TrustOptions.pem(Path.of("ca.crt")))
 *     .when().get("/hello")
 *     .then().statusCode(200);
 * }</pre>
 */
public sealed interface TrustOptions permits TrustOptions.Pem, TrustOptions.Jks, TrustOptions.Pkcs12 {

    /**
     * PEM trust store: a CA certificate file in PEM format.
     *
     * @param certPath path to the PEM-encoded CA certificate
     */
    static Pem pem(Path certPath) {
        return new Pem(certPath);
    }

    /**
     * JKS trust store.
     *
     * @param keyStorePath path to the JKS trust store file
     * @param password     trust store password
     */
    static Jks jks(Path keyStorePath, String password) {
        return new Jks(keyStorePath, password);
    }

    /**
     * PKCS12 (P12) trust store.
     *
     * @param keyStorePath path to the PKCS12 trust store file
     * @param password     trust store password
     */
    static Pkcs12 pkcs12(Path keyStorePath, String password) {
        return new Pkcs12(keyStorePath, password);
    }

    /** PEM CA certificate trust store. */
    record Pem(Path certPath) implements TrustOptions {}

    /** JKS trust store. */
    record Jks(Path keyStorePath, String password) implements TrustOptions {}

    /** PKCS12 trust store. */
    record Pkcs12(Path keyStorePath, String password) implements TrustOptions {}
}
