package io.smallrye.httpassured.integration.compat;

import tools.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * REST Assured compatibility tests — core DSL tutorial.
 *
 * <p>Mirrors the patterns from the Baeldung article
 * <a href="https://www.baeldung.com/rest-assured-tutorial">
 * A Guide to REST Assured</a>.
 *
 * <p><b>Unsupported features (REST Assured has them, http-assured does not yet):</b>
 * <ul>
 *   <li>{@code given().log().all()} / {@code then().log().all()} — no logging DSL</li>
 *   <li>{@code RestAssured.given()} as a static entry point — http-assured requires an instance</li>
 * </ul>
 * Backlog tasks have been filed for both gaps.
 */
@WireMockTest
class RestAssuredTutorialTest {

    private static final String USERS_JSON =
            "[{\"id\":1,\"name\":\"Alice\",\"age\":25}," +
            "{\"id\":2,\"name\":\"Bob\",\"age\":30}," +
            "{\"id\":3,\"name\":\"Charlie\",\"age\":22}]";

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

    // GET

    @Test
    void shouldGetUserList() {
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USERS_JSON)));

        client.given()
                .when().get("/users")
                .then()
                .statusCode(200)
                .body("$", hasSize(3));
    }

    @Test
    void shouldGetUserById() {
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Alice\",\"age\":25}")));

        client.given()
                .when().get("/users/{id}", 1)
                .then()
                .statusCode(200)
                .body("id", isEqualTo(1))
                .body("name", isEqualTo("Alice"));
    }

    @Test
    void shouldReturn404ForUnknownUser() {
        stubFor(get(urlEqualTo("/users/999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Not Found\"}")));

        client.given()
                .when().get("/users/{id}", 999)
                .then()
                .statusCode(404);
    }

    // POST

    @Test
    void shouldPostNewUser() {
        stubFor(post(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Location", "/users/42")
                        .withBody("{\"id\":42,\"name\":\"Dave\",\"age\":28}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Dave\",\"age\":28}")
                .when().post("/users")
                .then()
                .statusCode(201)
                .header("Location", "/users/42")
                .body("id", isEqualTo(42))
                .body("name", isEqualTo("Dave"));
    }

    @Test
    void shouldSetContentTypeForPost() {
        stubFor(post(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":99,\"name\":\"Eve\"}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Eve\"}")
                .when().post("/users")
                .then()
                .statusCode(201)
                .body("id", isNotNull());
    }

    @Test
    void shouldSerializePojoAsRequestBody() {
        stubFor(post(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":42,\"name\":\"Frank\",\"age\":35}")));

        // body(Object) auto-serializes to JSON and sets Content-Type
        User newUser = new User();
        newUser.name = "Frank";
        newUser.age = 35;

        client.given()
                .body(newUser)
                .when().post("/users")
                .then()
                .statusCode(201)
                .body("name", isEqualTo("Frank"));
    }

    // PUT

    @Test
    void shouldPutUser() {
        stubFor(put(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Alice Updated\",\"age\":26}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Alice Updated\",\"age\":26}")
                .when().put("/users/{id}", 1)
                .then()
                .statusCode(200)
                .body("name", isEqualTo("Alice Updated"));
    }

    // DELETE

    @Test
    void shouldDeleteUser() {
        stubFor(delete(urlEqualTo("/users/1"))
                .willReturn(aResponse().withStatus(204)));

        client.given()
                .when().delete("/users/{id}", 1)
                .then()
                .statusCode(204);
    }

    // PATCH

    @Test
    void shouldPatchUser() {
        stubFor(patch(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Alice\",\"age\":26}")));

        client.given()
                .contentType(ContentType.JSON)
                .body("{\"age\":26}")
                .when().patch("/users/{id}", 1)
                .then()
                .statusCode(200)
                .body("age", isEqualTo(26));
    }

    // when() shortcut and chaining

    @Test
    void shouldUseWhenShortcut() {
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USERS_JSON)));

        // client.when() is equivalent to client.given().when()
        client.when().get("/users")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldChainGivenWhenThen() {
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USERS_JSON)));

        client.given()
                .header("Accept", "application/json")
                .when().get("/users")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("$", hasSize(3));
    }

    // Query parameters

    @Test
    void shouldSendQueryParamInGreeting() {
        stubFor(get(urlPathEqualTo("/greeting"))
                .withQueryParam("name", equalTo("Alice"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"greeting\":\"Hello, Alice!\"}")));

        client.given()
                .queryParam("name", "Alice")
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body("greeting", isEqualTo("Hello, Alice!"));
    }

    // List deserialization

    @Test
    @SuppressWarnings("unchecked")
    void shouldDeserializeResponseListAsPojo() {
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USERS_JSON)));

        Response response = client.given().when().get("/users");
        // Use the generic deserialize(Type) path to decode a List<User>
        List<User> users = (List<User>) client.config().objectMapper()
                .deserialize(response.bodyAsBytes(), new TypeReference<List<User>>() {}.getType());
        assertNotNull(users);
        assertEquals(3, users.size());
        assertEquals("Alice", users.get(0).name);
    }

    // Inner model class

    public static class User {
        public int id;
        public String name;
        public int age;

        public User() {}
    }
}
