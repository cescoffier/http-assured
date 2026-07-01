package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REST Assured compatibility tests — Response object API.
 *
 * <p>Mirrors patterns from REST Assured's {@code ResponseITest}, focusing on
 * the {@link Response} object methods: status code, body, headers, and
 * deserialization.
 */
@WireMockTest
class ResponseTest {

    private static final String USER_JSON = "{\"id\":1,\"name\":\"Alice\",\"age\":25}";

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

    @Test
    void shouldGetStatusCodeFromResponseObject() {
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USER_JSON)));

        Response response = client.when().get("/users/1");
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldGetBodyAsStringFromResponse() {
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USER_JSON)));

        Response response = client.when().get("/users/1");
        String body = response.bodyAsString();
        assertNotNull(body);
        assertTrue(body.contains("\"name\":\"Alice\""));
    }

    @Test
    void shouldDeserializeBodyAsClass() {
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USER_JSON)));

        Response response = client.when().get("/users/1");
        User user = response.bodyAs(User.class);
        assertNotNull(user);
        assertEquals(1, user.id);
        assertEquals("Alice", user.name);
        assertEquals(25, user.age);
    }

    @Test
    void shouldGetHeadersFromResponse() {
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Request-Id", "req-42")
                        .withBody(USER_JSON)));

        Response response = client.when().get("/users/1");
        Headers headers = response.headers();
        assertNotNull(headers);
        assertTrue(headers.hasHeader("Content-Type"));
        assertEquals("req-42", headers.getValue("X-Request-Id").orElse(null));
    }

    @Test
    void shouldCallBodyAsStringMultipleTimes() {
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(USER_JSON)));

        Response response = client.when().get("/users/1");
        String first = response.bodyAsString();
        String second = response.bodyAsString();
        assertEquals(first, second, "bodyAsString() should return the same result on repeated calls");
    }

    // Inner model class

    public static class User {
        public int id;
        public String name;
        public int age;

        public User() {}
    }
}
