package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * REST Assured compatibility tests — JSON POST operations.
 *
 * <p>Mirrors the patterns from REST Assured's {@code JSONPostITest},
 * covering various body types (String, byte[], Object), automatic
 * content-type handling, and response extraction.
 *
 * <p>Complements {@link RestAssuredTutorialTest} which covers basic
 * POST with string body and POJO serialization against a {@code /users}
 * endpoint. This test class uses a different domain ({@code /orders})
 * and focuses on byte-array bodies, auto-content-type detection, and
 * extraction after validation.
 */
@WireMockTest
class JsonPostTest {

    private HttpAssured client;

    @BeforeEach
    void setupClient(WireMockRuntimeInfo wmInfo) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .build();
    }

    @AfterEach
    void closeClient() {
        if (client != null) client.close();
    }

    @Nested
    class BodySerialization {

        @Test
        void shouldPostJsonStringBody() {
            stubFor(post(urlEqualTo("/orders"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":10,\"product\":\"Laptop\",\"quantity\":2}")));

            client.given()
                    .contentType(ContentType.JSON)
                    .body("{\"product\":\"Laptop\",\"quantity\":2}")
                    .when().post("/orders")
                    .then()
                    .statusCode(201)
                    .body("id", isEqualTo(10))
                    .body("product", isEqualTo("Laptop"))
                    .body("quantity", isEqualTo(2));
        }

        @Test
        void shouldPostByteArrayBody() {
            stubFor(post(urlEqualTo("/orders"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":11,\"product\":\"Tablet\"}")));

            byte[] jsonBytes = "{\"product\":\"Tablet\"}".getBytes(StandardCharsets.UTF_8);

            client.given()
                    .contentType(ContentType.JSON)
                    .body(jsonBytes)
                    .when().post("/orders")
                    .then()
                    .statusCode(201)
                    .body("id", isEqualTo(11))
                    .body("product", isEqualTo("Tablet"));
        }

        @Test
        void shouldPostObjectBody() {
            stubFor(post(urlEqualTo("/orders"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":12,\"product\":\"Monitor\",\"quantity\":1}")));

            Order order = new Order();
            order.product = "Monitor";
            order.quantity = 1;

            client.given()
                    .body(order)
                    .when().post("/orders")
                    .then()
                    .statusCode(201)
                    .body("id", isEqualTo(12))
                    .body("product", isEqualTo("Monitor"))
                    .body("quantity", isEqualTo(1));
        }
    }

    @Nested
    class ContentTypeHandling {

        @Test
        void shouldAutoSetContentTypeForObjectBody() {
            stubFor(post(urlEqualTo("/orders"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":13,\"product\":\"Keyboard\"}")));

            Order order = new Order();
            order.product = "Keyboard";
            order.quantity = 3;

            // No explicit contentType() — auto-set for Object body
            client.given()
                    .body(order)
                    .when().post("/orders")
                    .then()
                    .statusCode(201);

            // Verify WireMock received the Content-Type header
            verify(postRequestedFor(urlEqualTo("/orders"))
                    .withHeader("Content-Type", containing("application/json")));
        }

        @Test
        void shouldPostWithExplicitContentType() {
            stubFor(post(urlEqualTo("/orders"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":14,\"product\":\"Mouse\"}")));

            client.given()
                    .contentType(ContentType.JSON)
                    .body("{\"product\":\"Mouse\",\"quantity\":5}")
                    .when().post("/orders")
                    .then()
                    .statusCode(201)
                    .body("id", isNotNull())
                    .body("product", isEqualTo("Mouse"));
        }
    }

    @Nested
    class ResponseExtraction {

        @Test
        void shouldPostAndExtractResponseId() {
            stubFor(post(urlEqualTo("/orders"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":99,\"product\":\"Headphones\",\"quantity\":1}")));

            int id = client.given()
                    .contentType(ContentType.JSON)
                    .body("{\"product\":\"Headphones\",\"quantity\":1}")
                    .when().post("/orders")
                    .then()
                    .statusCode(201)
                    .extract("id");

            assertEquals(99, id);
        }
    }

    // Inner model class

    public static class Order {
        public int id;
        public String product;
        public int quantity;

        public Order() {}
    }
}
