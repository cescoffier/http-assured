package io.smallrye.httpassured.config;

import io.smallrye.httpassured.spi.HttpClientEngine;
import io.smallrye.httpassured.spi.ObjectMapperProvider;
import io.smallrye.httpassured.spi.RawResponse;
import io.smallrye.httpassured.spi.RequestContext;
import io.smallrye.httpassured.spi.WebSocketContext;
import io.smallrye.httpassured.websocket.WsSession;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpAssuredConfigTest {

    /**
     * Minimal engine stub for config tests that don't need real HTTP.
     */
    private static final HttpClientEngine STUB_ENGINE = new HttpClientEngine() {
        @Override
        public RawResponse execute(RequestContext request) {
            return null;
        }

        @Override
        public Uni<RawResponse> executeAsync(RequestContext request) {
            return Uni.createFrom().nullItem();
        }

        @Override
        public Uni<WsSession> openWebSocket(WebSocketContext context) {
            return Uni.createFrom().nullItem();
        }

        @Override
        public void close() {
        }
    };

    private static final ObjectMapperProvider STUB_MAPPER = new ObjectMapperProvider() {
        @Override
        public byte[] serialize(Object object) {
            return new byte[0];
        }

        @Override
        public <T> T deserialize(byte[] body, Class<T> type) {
            return null;
        }

        @Override
        public Object deserialize(byte[] body, Type type) {
            return null;
        }
    };

    @Nested
    class ResolveUri {

        @Test
        void shouldResolveSimplePath() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .baseUri("http://localhost")
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals("http://localhost/users", config.resolveUri("/users"));
        }

        @Test
        void shouldResolveWithPort() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .baseUri("http://localhost")
                    .port(8080)
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals("http://localhost:8080/users", config.resolveUri("/users"));
        }

        @Test
        void shouldReplaceExistingPort() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .baseUri("http://localhost:3000")
                    .port(8080)
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals("http://localhost:8080/users", config.resolveUri("/users"));
        }

        @Test
        void shouldResolveWithBasePath() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .baseUri("http://localhost")
                    .basePath("/api/v1")
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals("http://localhost/api/v1/users", config.resolveUri("/users"));
        }

        @Test
        void shouldResolveWithPortAndBasePath() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .baseUri("http://localhost")
                    .port(8080)
                    .basePath("/api")
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals("http://localhost:8080/api/users", config.resolveUri("/users"));
        }

        @Test
        void shouldHandleEmptyPath() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .baseUri("http://localhost")
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals("http://localhost", config.resolveUri(""));
        }

        @Test
        void shouldHandleNullPath() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .baseUri("http://localhost")
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals("http://localhost", config.resolveUri(null));
        }

        @Test
        void shouldHandlePathWithoutLeadingSlash() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .baseUri("http://localhost")
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals("http://localhost/users", config.resolveUri("users"));
        }
    }

    @Nested
    class Defaults {

        @Test
        void shouldHaveDefaultTimeout() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals(Duration.ofSeconds(30), config.requestTimeout());
        }

        @Test
        void shouldHaveNoDefaultPort() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals(-1, config.port());
        }

        @Test
        void shouldHaveEmptyDefaultHeaders() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals(0, config.defaultHeaders().size());
        }
    }

    @Nested
    class DefaultHeaders {

        @Test
        void shouldAccumulateDefaultHeaders() {
            HttpAssuredConfig config = HttpAssuredConfig.builder()
                    .defaultHeader("Accept", "application/json")
                    .defaultHeader("X-Custom", "value")
                    .engine(STUB_ENGINE)
                    .objectMapper(STUB_MAPPER)
                    .build();

            assertEquals(2, config.defaultHeaders().size());
            assertEquals("application/json", config.defaultHeaders().getValue("Accept").orElse(null));
            assertEquals("value", config.defaultHeaders().getValue("X-Custom").orElse(null));
        }
    }

    @Nested
    class DefaultInstantiation {

        @Test
        void shouldBuildWithDefaults() {
            // VertxHttpEngine and JacksonObjectMapperProvider are used by default
            assertDoesNotThrow(() -> HttpAssuredConfig.builder().build().engine().close());
        }

        @Test
        void shouldAllowEngineOverride() {
            assertDoesNotThrow(() ->
                    HttpAssuredConfig.builder()
                            .engine(STUB_ENGINE)
                            .objectMapper(STUB_MAPPER)
                            .build());
        }
    }
}
