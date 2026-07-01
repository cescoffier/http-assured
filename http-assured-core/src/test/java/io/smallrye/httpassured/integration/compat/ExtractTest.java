package io.smallrye.httpassured.integration.compat;

import tools.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * REST Assured compatibility tests — extraction patterns.
 *
 * <p>Mirrors patterns from REST Assured's {@code GivenWhenThenExtractITest}.
 * Focuses on scenarios not covered by {@link ResponseExtractionTest}:
 * extracting after assertions, nested paths, array elements, chained requests,
 * and generic type deserialization.
 */
@WireMockTest
class ExtractTest {

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
    class ExtractAfterAssertion {

        @Test
        void shouldExtractAfterBodyAssertion() {
            stubFor(get(urlEqualTo("/employees/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":101,\"name\":\"Alice\",\"department\":\"Engineering\"}")));

            int id = client.given()
                    .when().get("/employees/1")
                    .then()
                    .statusCode(200)
                    .body("name", isEqualTo("Alice"))
                    .extract("id");
            assertEquals(101, id);
        }
    }

    @Nested
    class NestedPathExtraction {

        @Test
        void shouldExtractNestedPath() {
            String bookstoreJson = """
                    {
                      "store": {
                        "name": "City Books",
                        "book": [
                          {"title": "The Great Gatsby", "author": "F. Scott Fitzgerald", "price": 12.99},
                          {"title": "1984", "author": "George Orwell", "price": 9.99}
                        ]
                      }
                    }""";

            stubFor(get(urlEqualTo("/bookstore"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(bookstoreJson)));

            String author = client.given()
                    .when().get("/bookstore")
                    .then()
                    .statusCode(200)
                    .extract("store.book[0].author");
            assertEquals("F. Scott Fitzgerald", author);
        }

        @Test
        void shouldExtractArrayElement() {
            String teamsJson = """
                    [
                      {"name": "Alpha", "lead": "Dana"},
                      {"name": "Beta", "lead": "Erik"},
                      {"name": "Gamma", "lead": "Fiona"}
                    ]""";

            stubFor(get(urlEqualTo("/teams"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(teamsJson)));

            String secondTeamName = client.given()
                    .when().get("/teams")
                    .then()
                    .statusCode(200)
                    .extract("[1].name");
            assertEquals("Beta", secondTeamName);
        }
    }

    @Nested
    class ChainedRequests {

        @Test
        void shouldExtractAndUseInSubsequentRequest() {
            // First request: get a project and extract its owner ID
            stubFor(get(urlEqualTo("/projects/5"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":5,\"title\":\"Roadmap\",\"ownerId\":77}")));

            int ownerId = client.given()
                    .when().get("/projects/{id}", 5)
                    .then()
                    .statusCode(200)
                    .extract("ownerId");

            // Second request: use the extracted ID to fetch the owner
            stubFor(get(urlEqualTo("/users/77"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":77,\"name\":\"Grace\",\"role\":\"PM\"}")));

            String ownerName = client.given()
                    .when().get("/users/{id}", ownerId)
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(77))
                    .extract("name");
            assertEquals("Grace", ownerName);
        }
    }

    @Nested
    class TypeReferenceExtraction {

        @Test
        void shouldExtractAsTypeReference() {
            String itemsJson = """
                    [
                      {"sku": "A100", "label": "Bolt"},
                      {"sku": "B200", "label": "Nut"},
                      {"sku": "C300", "label": "Washer"}
                    ]""";

            stubFor(get(urlEqualTo("/inventory"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(itemsJson)));

            List<Item> items = client.given()
                    .when().get("/inventory")
                    .then()
                    .statusCode(200)
                    .extractAs(new TypeReference<List<Item>>() {});

            assertNotNull(items);
            assertEquals(3, items.size());
            assertEquals("A100", items.get(0).sku);
            assertEquals("Washer", items.get(2).label);
        }
    }

    // Inner model class

    public static class Item {
        public String sku;
        public String label;

        public Item() {}
    }
}
