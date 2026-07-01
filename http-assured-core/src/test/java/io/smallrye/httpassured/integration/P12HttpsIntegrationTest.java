package io.smallrye.httpassured.integration;

import io.smallrye.certs.CertificateFiles;
import io.smallrye.certs.Format;
import io.smallrye.certs.Pkcs12CertificateFiles;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.httpassured.spi.TrustOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;

import static io.smallrye.httpassured.integration.P12HttpsIntegrationTest.CERT_NAME;
import static io.smallrye.httpassured.integration.P12HttpsIntegrationTest.PASSWORD;

/**
 * HTTPS integration tests using PKCS12 (P12) certificates.
 */
@Certificates(
        baseDir = "target/certs/p12",
        certificates = @Certificate(name = CERT_NAME, formats = Format.PKCS12, password = PASSWORD))
class P12HttpsIntegrationTest extends AbstractHttpsTest {

    public static final String CERT_NAME = "p12-test";
    public static final String PASSWORD = "changeit";
    private static final Path CERT_DIR = Path.of("target/certs/p12");

    @BeforeAll
    static void startP12Server() throws InterruptedException {
        Pkcs12CertificateFiles p12 = new Pkcs12CertificateFiles(CERT_DIR, CERT_NAME, false, PASSWORD);
        certFiles = p12;
        HttpServerOptions serverOptions = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PfxOptions()
                        .setPath(p12.keyStoreFile().toString())
                        .setPassword(p12.password()));
        startServer(serverOptions);
    }

    @AfterAll
    static void stopP12Server() {
        stopServer();
    }

    @Override
    protected HttpServerOptions buildTlsOptions(CertificateFiles files) {
        Pkcs12CertificateFiles p12 = (Pkcs12CertificateFiles) files;
        return new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PfxOptions()
                        .setPath(p12.keyStoreFile().toString())
                        .setPassword(p12.password()));
    }

    @Override
    protected TrustOptions buildClientTrustOptions(CertificateFiles files) {
        Pkcs12CertificateFiles p12 = (Pkcs12CertificateFiles) files;
        return TrustOptions.pkcs12(p12.trustStoreFile(), p12.password());
    }
}
