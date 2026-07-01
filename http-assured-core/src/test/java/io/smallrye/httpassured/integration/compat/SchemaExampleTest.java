package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
public class SchemaExampleTest {

    private HttpAssured client;


    @BeforeEach
    void setupClient(WireMockRuntimeInfo wmInfo) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .build();

        stubFor(get(urlEqualTo("/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                   {
                                     "id": 1,
                                     "name": "Wireless Keyboard",
                                     "price": 49.99,
                                     "tags": ["electronics", "peripherals"],
                                     "dimensions": { "length": 45.0, "width": 15.0, "height": 3.5 }
                                   },
                                   {
                                     "id": 2,
                                     "name": "Ergonomic Mouse",
                                     "price": 29.95,
                                     "tags": ["electronics", "peripherals", "ergonomic"],
                                     "dimensions": { "length": 12.0, "width": 7.0, "height": 4.0 }
                                   },
                                   {
                                     "id": 3,
                                     "name": "USB-C Hub",
                                     "price": 34.50,
                                     "tags": ["electronics", "accessories"],
                                     "dimensions": { "length": 10.0, "width": 4.5, "height": 2.0 }
                                   }
                                 ]
                                """)));

        stubFor(get(urlEqualTo("/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [1,2,3]
                                """)));
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testValidation() {
        client.given().get("/products").then().matchesJsonSchema("schemas/products.json");
        client.given().get("/products").then().body("$", Assertions.matchesJsonSchema("schemas/products.json"));
    }

    @Test
    void testAnonymousRoot() {
        client.when().get("/json").then().body("$", Assertions.containsAll(1, 2, 3));
    }

    @Test
    void testTypeRef() {
        List<Map<String, Object>> products = client.given().get("/products").then().extractAs(new TypeReference<List<Map<String, Object>>>() {});
        assertThat(products).hasSize(3);
        assertThat(products.get(0).get("id")).isEqualTo(1);
        assertThat(products.get(0).get("name")).isEqualTo(("Wireless Keyboard"));
        assertThat(products.get(0).get("price")).isEqualTo(49.99);
    }

    record Product(int id, String name, double price, List<String> tags, Map<String, Double> dimensions) {}

    @Test
    void testTypeRef2() {
        List<Product> products = client.given().get("/products").then().extractAs(new TypeReference<List<Product>>() {});
        assertThat(products).hasSize(3);
        assertThat(products.get(0).id()).isEqualTo(1);
        assertThat(products.get(0).name()).isEqualTo(("Wireless Keyboard"));
        assertThat(products.get(0).price()).isEqualTo(49.99);
    }
}
