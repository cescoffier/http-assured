package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.allMatch;
import static io.smallrye.httpassured.assertion.Assertions.anyMatch;
import static io.smallrye.httpassured.assertion.Assertions.contains;
import static io.smallrye.httpassured.assertion.Assertions.containsAll;
import static io.smallrye.httpassured.assertion.Assertions.greaterThan;
import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;
import static io.smallrye.httpassured.assertion.Assertions.isNull;
import static io.smallrye.httpassured.assertion.Assertions.lessThan;

/**
 * REST Assured compatibility tests — asserting JSON responses.
 *
 * <p>Mirrors the patterns from the Baeldung article
 * <a href="https://www.baeldung.com/java-rest-assured-assert-json-responses">
 * Asserting JSON Responses with REST Assured</a>.
 *
 * <p><b>Unsupported features (REST Assured has them, http-assured does not yet):</b>
 * <ul>
 *   <li>Hamcrest matchers ({@code Matchers.equalTo}, {@code hasItem}, {@code everyItem},
 *       {@code closeTo}) — use http-assured's {@code allMatch}/{@code anyMatch}/{@code satisfies}
 *       as equivalents.</li>
 * </ul>
 * A backlog task has been filed for a Hamcrest {@code BodyAssertion} adapter.
 */
@WireMockTest
class JsonAssertionTest {

    private static final String STORE_JSON =
            "{\"store\":{\"book\":[" +
            "{\"title\":\"Sayings of the Century\",\"price\":8.95}," +
            "{\"title\":\"Sword of Honour\",\"price\":12.99}," +
            "{\"title\":\"The Lord of the Rings\",\"price\":22.99}" +
            "],\"bicycle\":{\"color\":\"red\",\"price\":19.95}}}";

    private static final String LOTTO_JSON =
            "{\"lotto\":{\"lottoId\":5,\"winning-numbers\":[2,45,34,23,7,5]," +
            "\"winners\":[{\"winnerId\":23,\"numbers\":[2,45,34,23,3,5]}," +
            "{\"winnerId\":54,\"numbers\":[52,3,12,11,18,22]}]}}";

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

    // Simple field assertions

    @Nested
    class SimpleFields {

        @Test
        void shouldAssertSimpleStringField() {
            stubFor(get(urlEqualTo("/user"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"John\",\"age\":30}")));

            client.given()
                    .when().get("/user")
                    .then()
                    .statusCode(200)
                    .body("name", isEqualTo("John"));
        }

        @Test
        void shouldAssertIntegerField() {
            stubFor(get(urlEqualTo("/user"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"John\",\"age\":30}")));

            client.given()
                    .when().get("/user")
                    .then()
                    .body("id", isEqualTo(1));
        }

        @Test
        void shouldAssertMultipleFieldsInChain() {
            stubFor(get(urlEqualTo("/user"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"John\",\"age\":30}")));

            client.given()
                    .when().get("/user")
                    .then()
                    .statusCode(200)
                    .body("id", isEqualTo(1))
                    .body("name", isEqualTo("John"))
                    .body("age", isEqualTo(30));
        }
    }

    // Nested object assertions

    @Nested
    class NestedObjects {

        @Test
        void shouldAssertNestedStringField() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .statusCode(200)
                    .body("store.bicycle.color", isEqualTo("red"));
        }

        @Test
        void shouldAssertNestedNumericField() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .body("store.bicycle.price", isEqualTo(19.95));
        }
    }

    // Array assertions

    @Nested
    class ArrayAssertions {

        @Test
        void shouldAssertArraySize() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .body("store.book", hasSize(3));
        }

        @Test
        void shouldAssertFirstArrayElement() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .body("store.book[0].title", isEqualTo("Sayings of the Century"));
        }

        @Test
        void shouldAssertRootArraySizeAndFirstElement() {
            stubFor(get(urlEqualTo("/users"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\":1,\"name\":\"John\",\"age\":30}," +
                                    "{\"id\":2,\"name\":\"Jane\",\"age\":25}," +
                                    "{\"id\":3,\"name\":\"Bob\",\"age\":17}]")));

            client.given()
                    .when().get("/users")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(3))
                    .body("[0].name", isEqualTo("John"));
        }

        @Test
        void shouldAssertListContainsElement() {
            stubFor(get(urlEqualTo("/lotto"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(LOTTO_JSON)));

            client.given()
                    .when().get("/lotto")
                    .then()
                    .body("lotto.winning-numbers", contains(45));
        }

        @Test
        void shouldAssertListContainsAllElements() {
            stubFor(get(urlEqualTo("/lotto"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(LOTTO_JSON)));

            client.given()
                    .when().get("/lotto")
                    .then()
                    .body("lotto.winning-numbers", containsAll(2, 45));
        }
    }

    // JsonPath filter predicates

    @Nested
    class JsonPathFilters {

        @Test
        void shouldFilterArrayByPredicateLowerBound() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .body("store.book[?(@.price < 10)]", hasSize(1));
        }

        @Test
        void shouldFilterArrayByPredicateUpperBound() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .body("store.book[?(@.price > 20)]", hasSize(1));
        }
    }

    // Collection-wide predicates

    @Nested
    class CollectionPredicates {

        @Test
        void shouldAssertAllPricesPositive() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .body("store.book[*].price", allMatch(p -> ((Number) p).doubleValue() > 0));
        }

        @Test
        void shouldAssertAnyTitleMatchesPredicate() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .body("store.book[*].title", anyMatch(t -> ((String) t).contains("Honour")));
        }
    }

    // Null / presence checks and numeric comparisons

    @Nested
    class NullAndNumeric {

        @Test
        void shouldAssertMissingFieldIsNull() {
            stubFor(get(urlEqualTo("/user"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"John\",\"age\":30}")));

            client.given()
                    .when().get("/user")
                    .then()
                    .body("nonexistent", isNull());
        }

        @Test
        void shouldAssertPresentFieldIsNotNull() {
            stubFor(get(urlEqualTo("/user"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"John\",\"age\":30}")));

            client.given()
                    .when().get("/user")
                    .then()
                    .body("name", isNotNull());
        }

        @Test
        void shouldAssertNumericGreaterThan() {
            stubFor(get(urlEqualTo("/user"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"John\",\"age\":30}")));

            client.given()
                    .when().get("/user")
                    .then()
                    .body("age", greaterThan(18));
        }

        @Test
        void shouldAssertNumericLessThan() {
            stubFor(get(urlEqualTo("/store"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(STORE_JSON)));

            client.given()
                    .when().get("/store")
                    .then()
                    .body("store.bicycle.price", lessThan(20.0));
        }
    }
}
