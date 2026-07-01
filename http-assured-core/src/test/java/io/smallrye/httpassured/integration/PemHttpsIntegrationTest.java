package io.smallrye.httpassured.integration;

import io.smallrye.certs.CertificateFiles;
import io.smallrye.certs.Format;
import io.smallrye.certs.PemCertificateFiles;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.httpassured.spi.TrustOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;

import static io.smallrye.httpassured.integration.PemHttpsIntegrationTest.CERT_NAME;

/**
 * HTTPS integration tests using PEM certificates.
 */
@Certificates(
        baseDir = "target/certs/pem",
        certificates = @Certificate(name = CERT_NAME, formats = Format.PEM))
class PemHttpsIntegrationTest extends AbstractHttpsTest {

    public static final String CERT_NAME = "pem-test";
    private static final Path CERT_DIR = Path.of("target/certs/pem");

    @BeforeAll
    static void startPemServer() throws InterruptedException {
        PemCertificateFiles pem = new PemCertificateFiles(CERT_DIR, CERT_NAME, false, null);
        certFiles = pem;
        HttpServerOptions serverOptions = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath(pem.certFile().toString())
                        .setKeyPath(pem.keyFile().toString()));
        startServer(serverOptions);
    }

    @AfterAll
    static void stopPemServer() {
        stopServer();
    }

    @Override
    protected HttpServerOptions buildTlsOptions(CertificateFiles files) {
        PemCertificateFiles pem = (PemCertificateFiles) files;
        return new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath(pem.certFile().toString())
                        .setKeyPath(pem.keyFile().toString()));
    }

    @Override
    protected TrustOptions buildClientTrustOptions(CertificateFiles files) {
        // trustStore() on a non-client PemCertificateFiles returns the CA cert file (pem-test-ca.crt)
        return TrustOptions.pem(files.trustStore());
    }
}
