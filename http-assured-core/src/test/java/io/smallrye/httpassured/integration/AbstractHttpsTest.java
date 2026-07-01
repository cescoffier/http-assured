package io.smallrye.httpassured.integration;

import io.smallrye.certs.CertificateFiles;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.HttpAssuredException;
import io.smallrye.httpassured.engine.vertx.VertxHttpEngine;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.spi.TrustOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract base for HTTPS integration tests.
 * <p>
 * Each concrete subclass starts its own embedded Vert.x HTTPS server using one certificate format
 * (PEM, JKS, or PKCS12) and inherits the full suite of test methods defined here.
 * </p>
 */
abstract class AbstractHttpsTest {

    protected static Vertx vertx;
    protected static HttpServer server;
    protected static HttpAssured trustedClient;
    protected static int httpsPort;

    /**
     * The {@link CertificateFiles} injected by the subclass {@code @BeforeAll}.
     * Used by {@link TlsCertificateValidation} tests that need the trust store path/password.
     */
    protected static CertificateFiles certFiles;

    protected static Router buildRouter(Vertx vertx) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/hello").handler(ctx ->
                ctx.response()
                        .putHeader("Content-Type", "text/plain")
                        .end("Hello HTTPS"));

        router.get("/users").handler(ctx ->
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end("[{\"name\":\"Alice\",\"age\":25},{\"name\":\"Bob\",\"age\":35}]"));

        router.get("/users/:id").handler(ctx -> {
            String id = ctx.pathParam("id");
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\"id\":" + id + ",\"name\":\"User-" + id + "\"}");
        });

        router.post("/users").handler(ctx ->
                ctx.response()
                        .setStatusCode(201)
                        .putHeader("Content-Type", "application/json")
                        .putHeader("Location", "/users/42")
                        .end("{\"id\":42,\"name\":\"created\"}"));

        router.put("/items/:id").handler(ctx -> {
            String id = ctx.pathParam("id");
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\"id\":" + id + ",\"updated\":true}");
        });

        router.delete("/items/:id").handler(ctx ->
                ctx.response().setStatusCode(204).end());

        router.patch("/items/:id").handler(ctx -> {
            String id = ctx.pathParam("id");
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\"id\":" + id + ",\"patched\":true}");
        });

        router.head("/health").handler(ctx ->
                ctx.response()
                        .putHeader("X-Status", "healthy")
                        .setStatusCode(200)
                        .end());

        router.options("/api").handler(ctx ->
                ctx.response()
                        .putHeader("Allow", "GET, POST, PUT, DELETE")
                        .setStatusCode(200)
                        .end());

        router.get("/echo-params").handler(ctx -> {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (var entry : ctx.queryParams()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(json.toString());
        });

        router.get("/echo-headers").handler(ctx -> {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (var entry : ctx.request().headers()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(json.toString());
        });

        return router;
    }

    /**
     * Called by each subclass to start the HTTPS server with the provided options.
     */
    protected static void startServer(HttpServerOptions serverOptions) throws InterruptedException {
        vertx = Vertx.vertx();
        Router router = buildRouter(vertx);

        CountDownLatch latch = new CountDownLatch(1);
        server = vertx.createHttpServer(serverOptions).requestHandler(router);
        server.listen(0).onComplete(ar -> {
            if (ar.succeeded()) {
                httpsPort = ar.result().actualPort();
            }
            latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "HTTPS server failed to start");

        trustedClient = HttpAssured.builder()
                .baseUri("https://localhost")
                .port(httpsPort)
                .engine(new VertxHttpEngine(vertx))
                .build();
    }

    /**
     * Called by each subclass to shut down the HTTPS server and client.
     */
    protected static void stopServer() {
        if (trustedClient != null) trustedClient.close();
        if (server != null) {
            CountDownLatch latch = new CountDownLatch(1);
            server.close().onComplete(v -> latch.countDown());
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close().onComplete(v -> latch.countDown());
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Subclasses implement this to build the Vert.x {@link HttpServerOptions} for their certificate format.
     */
    protected abstract HttpServerOptions buildTlsOptions(CertificateFiles files);

    /**
     * Subclasses implement this to build the matching {@link TrustOptions} for the client, given
     * the same {@link CertificateFiles}.
     */
    protected abstract TrustOptions buildClientTrustOptions(CertificateFiles files);

    @Nested
    class HttpsVerbs {

        @Test
        void shouldGetOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .when().get("/hello")
                    .then().statusCode(200).bodyEquals("Hello HTTPS");
        }

        @Test
        void shouldGetJsonArrayOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .when().get("/users")
                    .then()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("$", hasSize(2))
                    .body("[0].name", isEqualTo("Alice"));
        }

        @Test
        void shouldPostOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Dave\"}")
                    .when().post("/users")
                    .then()
                    .statusCode(201)
                    .header("Location", "/users/42")
                    .body("id", isEqualTo(42));
        }

        @Test
        void shouldPutOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Updated\"}")
                    .when().put("/items/{id}", 5)
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(5))
                    .body("updated", isEqualTo(true));
        }

        @Test
        void shouldDeleteOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .when().delete("/items/{id}", 3)
                    .then().statusCode(204);
        }

        @Test
        void shouldPatchOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .when().patch("/items/{id}", 7)
                    .then()
                    .statusCode(200)
                    .body("patched", isEqualTo(true));
        }

        @Test
        void shouldHeadOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .when().head("/health")
                    .then()
                    .statusCode(200)
                    .header("X-Status", "healthy");
        }

        @Test
        void shouldOptionsOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .when().options("/api")
                    .then()
                    .statusCode(200)
                    .header("Allow", "GET, POST, PUT, DELETE");
        }
    }

    @Nested
    class HttpsHeadersAndParams {

        @Test
        void shouldSendQueryParamsOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .queryParam("page", "2")
                    .queryParam("size", "10")
                    .when().get("/echo-params")
                    .then()
                    .statusCode(200)
                    .body("page", isEqualTo("2"))
                    .body("size", isEqualTo("10"));
        }

        @Test
        void shouldSendCustomHeadersOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .header("X-Request-Id", "test-123")
                    .when().get("/echo-headers")
                    .then()
                    .statusCode(200)
                    .body("X-Request-Id", isEqualTo("test-123"));
        }

        @Test
        void shouldSubstitutePathParamsOverHttps() {
            trustedClient.given()
                    .trustAll(true)
                    .when().get("/users/{id}", 42)
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(42))
                    .body("name", isEqualTo("User-42"));
        }
    }

    @Nested
    class TlsCertificateValidation {

        @Test
        void shouldSucceedWithTrustAllTrue() {
            HttpAssured client = HttpAssured.builder()
                    .baseUri("https://localhost").port(httpsPort)
                    .engine(new VertxHttpEngine(vertx))
                    .build();
            try {
                assertDoesNotThrow(() ->
                        client.given().trustAll(true)
                                .when().get("/hello")
                                .then().statusCode(200));
            } finally {
                client.close();
            }
        }

        @Test
        void shouldRejectUntrustedCert() {
            HttpAssured client = HttpAssured.builder()
                    .baseUri("https://localhost").port(httpsPort)
                    .engine(new VertxHttpEngine(vertx))
                    .build();
            try {
                assertThrows(HttpAssuredException.class, () ->
                        client.given().trustAll(false)
                                .when().get("/hello"));
            } finally {
                client.close();
            }
        }

        @Test
        void shouldSucceedWithMatchingFormatTrustStore() {
            HttpAssured client = HttpAssured.builder()
                    .baseUri("https://localhost").port(httpsPort)
                    .engine(new VertxHttpEngine(vertx))
                    .build();
            try {
                TrustOptions opts = buildClientTrustOptions(certFiles);
                assertDoesNotThrow(() ->
                        client.given().trustAll(false).trustOptions(opts)
                                .when().get("/hello")
                                .then().statusCode(200));
            } finally {
                client.close();
            }
        }
    }
}
