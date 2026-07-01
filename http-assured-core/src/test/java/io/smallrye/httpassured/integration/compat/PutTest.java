package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;

/**
 * REST Assured compatibility tests for PUT operations.
 *
 * <p>Covers patterns from REST Assured's {@code PutITest}, exercising
 * string, object, and byte-array request bodies as well as path-param
 * substitution on PUT endpoints.
 */
@WireMockTest
class PutTest {

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

    // PUT with string body

    @Test
    void shouldPutWithStringBody() {
        stubFor(put(urlEqualTo("/products/10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":10,\"name\":\"Widget\",\"price\":19.99}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Widget\",\"price\":19.99}")
                .when().put("/products/{id}", 10)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(10))
                .body("name", isEqualTo("Widget"))
                .body("price", isEqualTo(19.99));
    }

    // PUT with POJO body (auto-serialized)

    @Test
    void shouldPutWithObjectBody() {
        stubFor(put(urlEqualTo("/products/10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":10,\"name\":\"Gadget\",\"price\":29.99}")));

        Product updated = new Product();
        updated.name = "Gadget";
        updated.price = 29.99;

        client.given()
                .body(updated)
                .when().put("/products/{id}", 10)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(10))
                .body("name", isEqualTo("Gadget"))
                .body("price", isEqualTo(29.99));
    }

    // PUT with byte[] body

    @Test
    void shouldPutWithByteArrayBody() {
        stubFor(put(urlEqualTo("/products/10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":10,\"name\":\"Doohickey\",\"price\":9.99}")));

        byte[] body = "{\"name\":\"Doohickey\",\"price\":9.99}".getBytes(StandardCharsets.UTF_8);

        client.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/products/{id}", 10)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(10))
                .body("name", isEqualTo("Doohickey"));
    }

    // PUT returning full updated resource

    @Test
    void shouldPutAndReturnUpdatedResource() {
        stubFor(put(urlEqualTo("/items/5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":5,\"title\":\"Updated Title\",\"description\":\"New desc\",\"quantity\":42}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"title\":\"Updated Title\",\"description\":\"New desc\",\"quantity\":42}")
                .when().put("/items/{id}", 5)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(5))
                .body("title", isEqualTo("Updated Title"))
                .body("description", isEqualTo("New desc"))
                .body("quantity", isEqualTo(42));
    }

    // Inner model class

    public static class Product {
        public int id;
        public String name;
        public double price;

        public Product() {}
    }
}
