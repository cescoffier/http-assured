package io.smallrye.httpassured.integration;

import io.smallrye.certs.Format;
import io.smallrye.certs.PemCertificateFiles;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.engine.vertx.VertxHttpEngine;
import io.smallrye.httpassured.http.HttpVersion;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Certificates(
        baseDir = "target/certs/http-version",
        certificates = @Certificate(name = HttpVersionIntegrationTest.CERT_NAME, formats = Format.PEM))
class HttpVersionIntegrationTest {

    static final String CERT_NAME = "http-version-test";
    private static final Path CERT_DIR = Path.of("target/certs/http-version");

    private static Router buildVersionRouter(Vertx vertx) {
        Router router = Router.router(vertx);
        router.get("/version").handler(ctx ->
                ctx.response()
                        .putHeader("Content-Type", "text/plain")
                        .putHeader("X-Http-Version", ctx.request().version().name())
                        .end(ctx.request().version().name()));
        return router;
    }

    private static void awaitClose(Vertx vertx, HttpServer server) {
        if (server != null) {
            CountDownLatch latch = new CountDownLatch(1);
            server.close().onComplete(v -> latch.countDown());
            try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close().onComplete(v -> latch.countDown());
            try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
    }

    @Nested
    class Http10 {

        private static Vertx vertx;
        private static HttpServer server;
        private static HttpAssured client;
        private static int port;

        @BeforeAll
        static void start() throws InterruptedException {
            vertx = Vertx.vertx();
            Router router = buildVersionRouter(vertx);
            CountDownLatch latch = new CountDownLatch(1);
            server = vertx.createHttpServer().requestHandler(router);
            server.listen(0).onComplete(ar -> {
                if (ar.succeeded()) port = ar.result().actualPort();
                latch.countDown();
            });
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Server failed to start");
            client = HttpAssured.builder()
                    .baseUri("http://localhost").port(port)
                    .engine(new VertxHttpEngine(vertx))
                    .build();
        }

        @AfterAll
        static void stop() {
            if (client != null) client.close();
            awaitClose(vertx, server);
        }

        @Test
        void shouldSendHttp10Request() {
            client.given()
                    .version(HttpVersion.HTTP_1_0)
                    .when().get("/version")
                    .then()
                    .statusCode(200)
                    .httpVersion(HttpVersion.HTTP_1_0)
                    .bodyEquals("HTTP_1_0");
        }
    }

    @Nested
    class Http11 {

        private static Vertx vertx;
        private static HttpServer server;
        private static HttpAssured client;
        private static int port;

        @BeforeAll
        static void start() throws InterruptedException {
            vertx = Vertx.vertx();
            Router router = buildVersionRouter(vertx);
            CountDownLatch latch = new CountDownLatch(1);
            server = vertx.createHttpServer().requestHandler(router);
            server.listen(0).onComplete(ar -> {
                if (ar.succeeded()) port = ar.result().actualPort();
                latch.countDown();
            });
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Server failed to start");
            client = HttpAssured.builder()
                    .baseUri("http://localhost").port(port)
                    .engine(new VertxHttpEngine(vertx))
                    .build();
        }

        @AfterAll
        static void stop() {
            if (client != null) client.close();
            awaitClose(vertx, server);
        }

        @Test
        void shouldSendHttp11Request() {
            client.given()
                    .version(HttpVersion.HTTP_1_1)
                    .when().get("/version")
                    .then()
                    .statusCode(200)
                    .httpVersion(HttpVersion.HTTP_1_1)
                    .bodyEquals("HTTP_1_1");
        }

        @Test
        void shouldDefaultToHttp11() {
            client.given()
                    .when().get("/version")
                    .then()
                    .statusCode(200)
                    .httpVersion(HttpVersion.HTTP_1_1)
                    .bodyEquals("HTTP_1_1");
        }
    }

    @Nested
    class Http2 {

        private static Vertx vertx;
        private static HttpServer server;
        private static HttpAssured client;
        private static int port;

        @BeforeAll
        static void start() throws InterruptedException {
            vertx = Vertx.vertx();
            Router router = buildVersionRouter(vertx);

            PemCertificateFiles pem = new PemCertificateFiles(CERT_DIR, CERT_NAME, false, null);
            HttpServerOptions serverOptions = new HttpServerOptions()
                    .setSsl(true)
                    .setUseAlpn(true)
                    .setAlpnVersions(List.of(io.vertx.core.http.HttpVersion.HTTP_2, io.vertx.core.http.HttpVersion.HTTP_1_1))
                    .setKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(pem.certFile().toString())
                            .setKeyPath(pem.keyFile().toString()));

            CountDownLatch latch = new CountDownLatch(1);
            server = vertx.createHttpServer(serverOptions).requestHandler(router);
            server.listen(0).onComplete(ar -> {
                if (ar.succeeded()) port = ar.result().actualPort();
                latch.countDown();
            });
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Server failed to start");
            client = HttpAssured.builder()
                    .baseUri("https://localhost").port(port)
                    .engine(new VertxHttpEngine(vertx))
                    .build();
        }

        @AfterAll
        static void stop() {
            if (client != null) client.close();
            awaitClose(vertx, server);
        }

        @Test
        void shouldSendHttp2Request() {
            client.given()
                    .trustAll(true)
                    .version(HttpVersion.HTTP_2)
                    .when().get("/version")
                    .then()
                    .statusCode(200)
                    .httpVersion(HttpVersion.HTTP_2)
                    .bodyEquals("HTTP_2");
        }
    }

    @Nested
    class Http3 {

        private static Vertx vertx;
        private static HttpServer server;
        private static HttpAssured client;
        private static int quicPort;

        @BeforeAll
        static void start() throws InterruptedException {
            vertx = Vertx.vertx();
            Router router = buildVersionRouter(vertx);

            PemCertificateFiles pem = new PemCertificateFiles(CERT_DIR, CERT_NAME, false, null);
            HttpServerConfig serverConfig = new HttpServerConfig()
                    .setVersions(io.vertx.core.http.HttpVersion.HTTP_3);
            ServerSSLOptions sslOpts = new ServerSSLOptions()
                    .setKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(pem.certFile().toString())
                            .setKeyPath(pem.keyFile().toString()));

            CountDownLatch latch = new CountDownLatch(1);
            server = vertx.httpServerBuilder()
                    .with(serverConfig)
                    .with(sslOpts)
                    .build()
                    .requestHandler(router);
            server.listen(0).onComplete(ar -> {
                if (ar.succeeded()) {
                    quicPort = ar.result().actualPort();
                }
                latch.countDown();
            });
            assertTrue(latch.await(10, TimeUnit.SECONDS), "HTTP/3 server failed to start");
            client = HttpAssured.builder()
                    .baseUri("https://localhost").port(quicPort)
                    .engine(new VertxHttpEngine(vertx))
                    .build();
        }

        @AfterAll
        static void stop() {
            if (client != null) client.close();
            awaitClose(vertx, server);
        }

        @Test
        void shouldSendHttp3Request() {
            client.given()
                    .trustAll(true)
                    .version(HttpVersion.HTTP_3)
                    .when().get("/version")
                    .then()
                    .statusCode(200)
                    .httpVersion(HttpVersion.HTTP_3)
                    .bodyEquals("HTTP_3");
        }
    }
}
