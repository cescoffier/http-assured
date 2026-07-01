package io.smallrye.httpassured.integration.compat;

import tools.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * REST Assured compatibility tests -- object mapping (serialization and deserialization).
 *
 * <p>Exercises {@code body(Object)} auto-serialization and the {@code bodyAs} /
 * {@code extractAs} deserialization paths with nested POJOs and generic types.
 *
 * <p>Complements {@link ResponseExtractionTest} (flat POJO, JsonPath extraction)
 * and {@link RestAssuredTutorialTest} (simple POJO POST, list via raw ObjectMapper).
 */
@WireMockTest
class ObjectMappingTest {

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

    // Serialization

    /**
     * Sends a complex POJO with a nested object as the request body.
     * Verifies the server receives correctly serialized JSON via WireMock's
     * {@code equalToJson} request matcher.
     */
    @Test
    void shouldSerializePojoToJsonRequestBody() {
        String expectedJson = """
                {
                  "id": 0,
                  "name": "Wireless Mouse",
                  "price": 29.99,
                  "category": {
                    "id": 5,
                    "name": "Peripherals"
                  }
                }
                """;

        stubFor(post(urlEqualTo("/products"))
                .withRequestBody(equalToJson(expectedJson, true, false))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":42,\"name\":\"Wireless Mouse\",\"price\":29.99," +
                                "\"category\":{\"id\":5,\"name\":\"Peripherals\"}}")));

        Category peripherals = new Category();
        peripherals.id = 5;
        peripherals.name = "Peripherals";

        Product product = new Product();
        product.name = "Wireless Mouse";
        product.price = 29.99;
        product.category = peripherals;

        client.given()
                .body(product)
                .when().post("/products")
                .then()
                .statusCode(201)
                .body("id", isEqualTo(42))
                .body("name", isEqualTo("Wireless Mouse"));
    }

    // Deserialization -- nested POJO

    /**
     * Deserializes a JSON response containing a nested object into a POJO
     * using {@code Response.bodyAs(Class)}.
     */
    @Test
    void shouldDeserializeResponseToNestedPojo() {
        stubFor(get(urlEqualTo("/products/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Mechanical Keyboard\",\"price\":149.95," +
                                "\"category\":{\"id\":3,\"name\":\"Keyboards\"}}")));

        Response response = client.given().when().get("/products/1");
        Product product = response.bodyAs(Product.class);

        assertEquals(1, product.id);
        assertEquals("Mechanical Keyboard", product.name);
        assertEquals(149.95, product.price, 0.001);
        assertNotNull(product.category);
        assertEquals(3, product.category.id);
        assertEquals("Keyboards", product.category.name);
    }

    // Deserialization -- generic type via TypeReference

    /**
     * Deserializes a JSON array response into {@code List<Product>} using
     * {@code Response.bodyAs(TypeReference)}.
     */
    @Test
    void shouldExtractAsGenericType() {
        stubFor(get(urlEqualTo("/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[" +
                                "{\"id\":1,\"name\":\"Mouse\",\"price\":25.0,\"category\":{\"id\":1,\"name\":\"Peripherals\"}}," +
                                "{\"id\":2,\"name\":\"Monitor\",\"price\":350.0,\"category\":{\"id\":2,\"name\":\"Displays\"}}," +
                                "{\"id\":3,\"name\":\"Webcam\",\"price\":75.0,\"category\":{\"id\":1,\"name\":\"Peripherals\"}}" +
                                "]")));

        List<Product> products = client.given()
                .when().get("/products")
                .bodyAs(new TypeReference<List<Product>>() {});

        assertNotNull(products);
        assertEquals(3, products.size());
        assertEquals("Mouse", products.get(0).name);
        assertEquals("Displays", products.get(1).category.name);
        assertEquals(75.0, products.get(2).price, 0.001);
    }

    // Round-trip: POST then GET

    /**
     * POSTs a POJO, then GETs it back and compares the deserialized result
     * with the original.
     */
    @Test
    void shouldRoundTripPojoThroughPostAndGet() {
        Category storage = new Category();
        storage.id = 10;
        storage.name = "Storage";

        Product original = new Product();
        original.id = 0;
        original.name = "SSD 1TB";
        original.price = 89.99;
        original.category = storage;

        // POST creates the product (server assigns id=77)
        stubFor(post(urlEqualTo("/products"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":77,\"name\":\"SSD 1TB\",\"price\":89.99," +
                                "\"category\":{\"id\":10,\"name\":\"Storage\"}}")));

        Product created = client.given()
                .body(original)
                .when().post("/products")
                .then()
                .statusCode(201)
                .extractAs(Product.class);

        assertEquals(77, created.id);
        assertEquals(original.name, created.name);
        assertEquals(original.price, created.price, 0.001);

        // GET retrieves the same product by the assigned id
        stubFor(get(urlEqualTo("/products/77"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":77,\"name\":\"SSD 1TB\",\"price\":89.99," +
                                "\"category\":{\"id\":10,\"name\":\"Storage\"}}")));

        Product fetched = client.given()
                .when().get("/products/{id}", created.id)
                .bodyAs(Product.class);

        assertEquals(created.id, fetched.id);
        assertEquals(created.name, fetched.name);
        assertEquals(created.price, fetched.price, 0.001);
        assertNotNull(fetched.category);
        assertEquals(created.category.id, fetched.category.id);
        assertEquals(created.category.name, fetched.category.name);
    }

    // Inner model classes

    public static class Product {
        public int id;
        public String name;
        public double price;
        public Category category;

        public Product() {}
    }

    public static class Category {
        public int id;
        public String name;

        public Category() {}
    }
}
