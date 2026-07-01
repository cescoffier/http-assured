package io.smallrye.httpassured.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.spec.RequestSpec;
import io.smallrye.httpassured.spec.ResponseSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.options;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.allMatch;
import static io.smallrye.httpassured.assertion.Assertions.contains;
import static io.smallrye.httpassured.assertion.Assertions.containsAll;
import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;
import static io.smallrye.httpassured.assertion.Assertions.isNull;
import static io.smallrye.httpassured.assertion.Assertions.satisfies;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the full DSL against a WireMock HTTP stub server.
 */
@WireMockTest
class HttpIntegrationTest {

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
    class GetRequests {

        @Test
        void shouldGetPlainText() {
            stubFor(get(urlEqualTo("/hello"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("Hello World")));

            client.given()
                    .when()
                    .get("/hello")
                    .then()
                    .statusCode(200)
                    .bodyEquals("Hello World");
        }

        @Test
        void shouldGetJsonArray() {
            stubFor(get(urlEqualTo("/users"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"name\":\"Alice\",\"age\":25},{\"name\":\"Bob\",\"age\":35},{\"name\":\"Charlie\",\"age\":17}]")));

            client.given()
                    .when()
                    .get("/users")
                    .then()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("$", hasSize(3))
                    .body("[0].name", isEqualTo("Alice"))
                    .body("[1].age", isEqualTo(35));
        }

        @Test
        void shouldGetWithPathParam() {
            stubFor(get(urlEqualTo("/users/5"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":5,\"name\":\"User-5\",\"age\":30}")));

            client.given()
                    .when()
                    .get("/users/{id}", 5)
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(5))
                    .body("name", isEqualTo("User-5"));
        }

        @Test
        void shouldGetWithNamedPathParam() {
            stubFor(get(urlEqualTo("/users/7"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":7,\"name\":\"User-7\",\"age\":30}")));

            client.given()
                    .pathParam("id", "7")
                    .when()
                    .get("/users/{id}")
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(7))
                    .body("name", isEqualTo("User-7"));
        }

        @Test
        void shouldGetWithQueryParams() {
            stubFor(get(urlPathEqualTo("/echo-params"))
                    .withQueryParam("page", equalTo("2"))
                    .withQueryParam("size", equalTo("10"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"page\":\"2\",\"size\":\"10\"}")));

            client.given()
                    .queryParam("page", "2")
                    .queryParam("size", "10")
                    .when()
                    .get("/echo-params")
                    .then()
                    .statusCode(200)
                    .body("page", isEqualTo("2"))
                    .body("size", isEqualTo("10"));
        }

        @Test
        void shouldGetWithCustomHeaders() {
            stubFor(get(urlEqualTo("/echo-headers"))
                    .withHeader("X-Custom", equalTo("test-value"))
                    .withHeader("X-Request-Id", equalTo("abc-123"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"X-Custom\":\"test-value\",\"X-Request-Id\":\"abc-123\"}")));

            client.given()
                    .header("X-Custom", "test-value")
                    .header("X-Request-Id", "abc-123")
                    .when()
                    .get("/echo-headers")
                    .then()
                    .statusCode(200)
                    .body("X-Custom", isEqualTo("test-value"))
                    .body("X-Request-Id", isEqualTo("abc-123"));
        }
    }

    @Nested
    class PostRequests {

        @Test
        void shouldPostJsonString() {
            stubFor(post(urlEqualTo("/users"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("Location", "/users/42")
                            .withBody("{\"name\":\"Dave\",\"age\":28,\"id\":42}")));

            client.given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Dave\",\"age\":28}")
                    .when()
                    .post("/users")
                    .then()
                    .statusCode(201)
                    .header("Location", "/users/42")
                    .body("name", isEqualTo("Dave"))
                    .body("age", isEqualTo(28))
                    .body("id", isEqualTo(42));
        }

        @Test
        void shouldPostSerializedObject() {
            stubFor(post(urlEqualTo("/users"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"Eve\",\"age\":22,\"id\":42}")));

            var user = new TestUser("Eve", 22);
            client.given()
                    .body(user)
                    .when()
                    .post("/users")
                    .then()
                    .statusCode(201)
                    .body("name", isEqualTo("Eve"))
                    .body("id", isEqualTo(42));
        }
    }

    @Nested
    class PutRequests {

        @Test
        void shouldPutJson() {
            stubFor(put(urlEqualTo("/users/10"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"Updated\",\"id\":10}")));

            client.given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Updated\"}")
                    .when()
                    .put("/users/{id}", 10)
                    .then()
                    .statusCode(200)
                    .body("name", isEqualTo("Updated"))
                    .body("id", isEqualTo(10));
        }
    }

    @Nested
    class DeleteRequests {

        @Test
        void shouldDeleteUser() {
            stubFor(delete(urlEqualTo("/users/10"))
                    .willReturn(aResponse().withStatus(204)));

            client.given()
                    .when()
                    .delete("/users/{id}", 10)
                    .then()
                    .statusCode(204);
        }
    }

    @Nested
    class PatchRequests {

        @Test
        void shouldPatchUser() {
            stubFor(patch(urlEqualTo("/users/3"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"Patched\",\"id\":3}")));

            client.given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Patched\"}")
                    .when()
                    .patch("/users/{id}", 3)
                    .then()
                    .statusCode(200)
                    .body("name", isEqualTo("Patched"))
                    .body("id", isEqualTo(3));
        }
    }

    @Nested
    class HeadRequests {

        @Test
        void shouldHead() {
            stubFor(head(urlEqualTo("/health"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("X-Status", "healthy")));

            client.given()
                    .when()
                    .head("/health")
                    .then()
                    .statusCode(200)
                    .header("X-Status", "healthy");
        }
    }

    @Nested
    class OptionsRequests {

        @Test
        void shouldOptions() {
            stubFor(options(urlEqualTo("/api"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Allow", "GET, POST, PUT, DELETE")));

            client.given()
                    .when()
                    .options("/api")
                    .then()
                    .statusCode(200)
                    .header("Allow", "GET, POST, PUT, DELETE");
        }
    }

    @Nested
    class JsonPathAssertions {

        @Test
        void shouldAssertNestedJsonPath() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"book\":[{\"title\":\"Book A\",\"price\":8.95},{\"title\":\"Book B\",\"price\":12.99},{\"title\":\"Book C\",\"price\":22.99}],\"bicycle\":{\"color\":\"red\",\"price\":19.95}}}")));

            client.given()
                    .when()
                    .get("/store")
                    .then()
                    .statusCode(200)
                    .body("store.bicycle.color", isEqualTo("red"))
                    .body("store.book[0].title", isEqualTo("Book A"))
                    .body("store.book", hasSize(3));
        }

        @Test
        void shouldUseStandardJsonPathSyntax() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"book\":[{\"title\":\"Book A\",\"price\":8.95},{\"title\":\"Book B\",\"price\":12.99},{\"title\":\"Book C\",\"price\":22.99}]}}")));

            client.given()
                    .when()
                    .get("/store")
                    .then()
                    .body("$.store.book[*].price", allMatch(p -> ((Number) p).doubleValue() > 0))
                    .body("$.store.book[*].title", hasSize(3));
        }

        @Test
        void shouldFilterWithJsonPath() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"book\":[{\"title\":\"Book A\",\"price\":8.95},{\"title\":\"Book B\",\"price\":12.99},{\"title\":\"Book C\",\"price\":22.99}]}}")));

            client.given()
                    .when()
                    .get("/store")
                    .then()
                    .body("$.store.book[?(@.price < 10)]", hasSize(1))
                    .body("$.store.book[?(@.price > 20)]", hasSize(1));
        }

        @Test
        void shouldExtractValue() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"User-1\",\"age\":30}")));

            Response response = client.given()
                    .when()
                    .get("/users/1");

            String name = response.then().extract("name");
            assertEquals("User-1", name);
        }

        @Test
        void shouldExtractAsType() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"User-1\",\"age\":30}")));

            TestUser user = client.given()
                    .when()
                    .get("/users/1")
                    .then()
                    .statusCode(200)
                    .extractAs(TestUser.class);

            assertEquals("User-1", user.name);
            assertEquals(30, user.age);
        }
    }

    @Nested
    class AssertionCombinators {

        @Test
        void shouldUseSatisfiesPredicate() {
            stubFor(get(urlEqualTo("/users"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"name\":\"Alice\",\"age\":25}]")));

            client.given()
                    .when()
                    .get("/users")
                    .then()
                    .body("[0].age", satisfies(a -> (int) a >= 18));
        }

        @Test
        void shouldUseContains() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"book\":[{\"title\":\"Book A\"},{\"title\":\"Book B\"},{\"title\":\"Book C\"}]}}")));

            client.given()
                    .when()
                    .get("/store")
                    .then()
                    .body("$.store.book[*].title", contains("Book B"));
        }

        @Test
        void shouldUseContainsAll() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"store\":{\"book\":[{\"title\":\"Book A\"},{\"title\":\"Book B\"},{\"title\":\"Book C\"}]}}")));

            client.given()
                    .when()
                    .get("/store")
                    .then()
                    .body("$.store.book[*].title", containsAll("Book A", "Book C"));
        }

        @Test
        void shouldUseIsNotNull() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"User-1\",\"age\":30}")));

            client.given()
                    .when()
                    .get("/users/1")
                    .then()
                    .body("name", isNotNull());
        }

        @Test
        void shouldUseIsNull() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"User-1\",\"age\":30}")));

            client.given()
                    .when()
                    .get("/users/1")
                    .then()
                    .body("nonexistent", isNull());
        }
    }

    @Nested
    class SpecificationReuse {

        @Test
        void shouldApplyRequestSpec() {
            stubFor(get(urlPathEqualTo("/echo-headers"))
                    .withHeader("X-Custom", equalTo("from-spec"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"X-Custom\":\"from-spec\"}")));

            RequestSpec spec = RequestSpec.builder()
                    .header("X-Custom", "from-spec")
                    .queryParam("page", "3")
                    .build();

            client.given()
                    .spec(spec)
                    .when()
                    .get("/echo-headers")
                    .then()
                    .statusCode(200)
                    .body("X-Custom", isEqualTo("from-spec"));
        }

        @Test
        void shouldApplyResponseSpec() {
            stubFor(get(urlEqualTo("/hello"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("Hello World")));

            ResponseSpec okSpec = ResponseSpec.builder()
                    .statusCode(200)
                    .build();

            client.given()
                    .when()
                    .get("/hello")
                    .then()
                    .spec(okSpec);
        }

        @Test
        void shouldCombineSpecWithInlineConfig() {
            stubFor(get(urlEqualTo("/echo-headers"))
                    .withHeader("Authorization", equalTo("Bearer test-token"))
                    .withHeader("X-Extra", equalTo("additional"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"Authorization\":\"Bearer test-token\",\"X-Extra\":\"additional\"}")));

            RequestSpec authSpec = RequestSpec.builder()
                    .header("Authorization", "Bearer test-token")
                    .build();

            client.given()
                    .spec(authSpec)
                    .header("X-Extra", "additional")
                    .when()
                    .get("/echo-headers")
                    .then()
                    .statusCode(200)
                    .body("Authorization", isEqualTo("Bearer test-token"))
                    .body("X-Extra", isEqualTo("additional"));
        }
    }

    @Nested
    class ResponseAccess {

        @Test
        void shouldAccessStatusCode() {
            stubFor(get(urlEqualTo("/hello"))
                    .willReturn(aResponse().withStatus(200).withBody("Hello World")));

            Response response = client.given().when().get("/hello");
            assertEquals(200, response.statusCode());
        }

        @Test
        void shouldAccessHeaders() {
            stubFor(get(urlEqualTo("/hello"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("Hello World")));

            Response response = client.given().when().get("/hello");
            assertTrue(response.headers().hasHeader("Content-Type"));
        }

        @Test
        void shouldAccessBodyAsString() {
            stubFor(get(urlEqualTo("/hello"))
                    .willReturn(aResponse().withStatus(200).withBody("Hello World")));

            Response response = client.given().when().get("/hello");
            assertEquals("Hello World", response.bodyAsString());
        }

        @Test
        void shouldDeserializeBody() {
            stubFor(get(urlEqualTo("/users/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"User-1\",\"age\":30}")));

            TestUser user = client.given().when().get("/users/1").bodyAs(TestUser.class);
            assertEquals("User-1", user.name);
        }
    }

    @Nested
    class WhenShortcut {

        @Test
        void shouldWorkWithWhenDirectly() {
            stubFor(get(urlEqualTo("/hello"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("Hello World")));

            client.when()
                    .get("/hello")
                    .then()
                    .statusCode(200)
                    .bodyEquals("Hello World");
        }
    }

    public static class TestUser {
        public String name;
        public int age;
        public int id;

        public TestUser() {
        }

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
