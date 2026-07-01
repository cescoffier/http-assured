package io.smallrye.httpassured.integration;

import io.smallrye.certs.CertificateFiles;
import io.smallrye.certs.Format;
import io.smallrye.certs.JksCertificateFiles;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.httpassured.spi.TrustOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;

import static io.smallrye.httpassured.integration.JksHttpsIntegrationTest.CERT_NAME;

/**
 * HTTPS integration tests using JKS certificates.
 */
@Certificates(
        baseDir = "target/certs/jks",
        certificates = @Certificate(name = CERT_NAME, formats = Format.JKS, password = JksHttpsIntegrationTest.PASSWORD))
class JksHttpsIntegrationTest extends AbstractHttpsTest {

    public static final String CERT_NAME = "jks-test";
    public static final String PASSWORD = "changeit";
    public static final Path CERT_DIR = Path.of("target/certs/jks");

    @BeforeAll
    static void startJksServer() throws InterruptedException {
        JksCertificateFiles jks = new JksCertificateFiles(CERT_DIR, CERT_NAME, false, PASSWORD);
        certFiles = jks;
        HttpServerOptions serverOptions = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new JksOptions()
                        .setPath(jks.keyStoreFile().toString())
                        .setPassword(jks.password()));
        startServer(serverOptions);
    }

    @AfterAll
    static void stopJksServer() {
        stopServer();
    }

    @Override
    protected HttpServerOptions buildTlsOptions(CertificateFiles files) {
        JksCertificateFiles jks = (JksCertificateFiles) files;
        return new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new JksOptions()
                        .setPath(jks.keyStoreFile().toString())
                        .setPassword(jks.password()));
    }

    @Override
    protected TrustOptions buildClientTrustOptions(CertificateFiles files) {
        JksCertificateFiles jks = (JksCertificateFiles) files;
        return TrustOptions.jks(jks.trustStoreFile(), jks.password());
    }
}
