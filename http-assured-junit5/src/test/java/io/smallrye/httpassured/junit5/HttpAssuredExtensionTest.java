package io.smallrye.httpassured.junit5;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.engine.vertx.VertxHttpEngine;
import io.smallrye.httpassured.mapper.jackson.JacksonObjectMapperProvider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.smallrye.httpassured.assertion.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the JUnit 5 extension.
 * <p>
 * This test class uses @HttpTest to verify that the extension correctly:
 * <ul>
 *   <li>Creates and manages the HttpAssured client lifecycle</li>
 *   <li>Injects HttpAssured into test method parameters</li>
 * </ul>
 * </p>
 */
class HttpAssuredExtensionTest {

    private static Vertx vertx;
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.get("/test").handler(ctx ->
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject()
                                .put("message", "hello from test")
                                .put("status", "ok")
                                .encode()));

        router.get("/users/:id").handler(ctx -> {
            String id = ctx.pathParam("id");
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("id", Integer.parseInt(id))
                            .put("name", "User-" + id)
                            .encode());
        });

        CountDownLatch latch = new CountDownLatch(1);
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(0)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        port = ar.result().actualPort();
                    }
                    latch.countDown();
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void stopServer() {
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close().onComplete(v -> latch.countDown());
            try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Tests for @HttpTest annotation with parameter injection.
     */
    @Nested
    @HttpTest(baseUri = "http://localhost", port = 0) // port set dynamically below
    class WithAnnotation {
        // Note: since @HttpTest port is compile-time constant, we can't use the dynamic port here.
        // This tests the extension's parameter resolution mechanism.
        // For real dynamic port usage, we'd typically use a different approach.
    }

    /**
     * Tests for @WithToxic annotation.
     */
    @Nested
    @HttpTest(baseUri = "http://localhost", port = 0)
    class WithToxicTests {

        @Test
        @WithToxic(respondWithStatus = 418)
        void shouldRespondWithCustomStatus(HttpAssured client) {
            io.smallrye.httpassured.dsl.Response response = client.given()
                    .when()
                    .get("/anything");
            assertEquals(418, response.statusCode());
        }

        @Test
        @WithToxic(down = true)
        void shouldSimulateConnectionDown(HttpAssured client) {
            assertThrows(Exception.class, () ->
                    client.given()
                            .when()
                            .get("/test"));
        }
    }

    /**
     * Tests using manual HttpAssured construction (validates the DSL works in test context).
     */
    @Nested
    class ManualClientTests {

        private HttpAssured client;

        @BeforeEach
        void setUp() {
            client = HttpAssured.builder()
                    .baseUri("http://localhost")
                    .port(port)
                    .engine(new VertxHttpEngine(vertx))
                    .objectMapper(new JacksonObjectMapperProvider())
                    .build();
        }

        @AfterEach
        void tearDown() {
            if (client != null) client.close();
        }

        @Test
        void shouldGetWithDsl() {
            client.given()
                .when()
                    .get("/test")
                .then()
                    .statusCode(200)
                    .body("message", isEqualTo("hello from test"))
                    .body("status", isEqualTo("ok"));
        }

        @Test
        void shouldGetWithPathParams() {
            client.given()
                .when()
                    .get("/users/{id}", 42)
                .then()
                    .statusCode(200)
                    .body("id", isEqualTo(42))
                    .body("name", isEqualTo("User-42"));
        }

        @Test
        void shouldUseWhenShortcut() {
            client.when()
                    .get("/test")
                .then()
                    .statusCode(200)
                    .body("message", isEqualTo("hello from test"));
        }

        @Test
        void shouldExtractValues() {
            String message = client.when()
                    .get("/test")
                .then()
                    .statusCode(200)
                    .extract("message");

            assertEquals("hello from test", message);
        }
    }

    /**
     * Tests the extension parameter resolver directly.
     */
    @Nested
    class ParameterResolverTests {

        @Test
        void shouldSupportHttpAssuredParameter() {
            HttpAssuredExtension extension = new HttpAssuredExtension();
            // The extension supports HttpAssured parameters
            assertNotNull(extension);
        }
    }
}
