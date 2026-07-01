package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;

/**
 * REST Assured compatibility tests -- path parameter substitution.
 *
 * <p>Complements the basic positional, named, and multiple-named cases
 * already covered in {@link HeadersParametersTest.PathParameters} with
 * additional edge cases: multiple positional params, mid-path
 * substitution, and numeric values.
 *
 * <p><b>Note:</b> Mixing named {@code pathParam()} with positional
 * varargs in the same request is <em>not</em> supported. When positional
 * values are supplied, named params are ignored during substitution.
 */
@WireMockTest
class PathParamTest {

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
    void shouldSubstituteMultiplePositionalParams() {
        stubFor(get(urlEqualTo("/users/1/posts/42"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"userId\":1,\"postId\":42}")));

        client.given()
                .when().get("/users/{userId}/posts/{postId}", 1, 42)
                .then()
                .statusCode(200)
                .body("userId", isEqualTo(1))
                .body("postId", isEqualTo(42));
    }

    @Test
    void shouldSubstitutePositionalParamWithStringValue() {
        stubFor(get(urlEqualTo("/users/alice/profile"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"username\":\"alice\"}")));

        client.given()
                .when().get("/users/{username}/profile", "alice")
                .then()
                .statusCode(200)
                .body("username", isEqualTo("alice"));
    }

    @Test
    void shouldSubstitutePathParamInMiddleOfPath() {
        stubFor(get(urlEqualTo("/api/v2/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"version\":\"v2\"}")));

        client.given()
                .pathParam("version", "v2")
                .when().get("/api/{version}/users")
                .then()
                .statusCode(200)
                .body("version", isEqualTo("v2"));
    }

    @Test
    void shouldHandleNumericPathParam() {
        stubFor(get(urlEqualTo("/items/999"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":999,\"name\":\"Widget\"}")));

        client.given()
                .pathParam("id", "999")
                .when().get("/items/{id}")
                .then()
                .statusCode(200)
                .body("id", isEqualTo(999))
                .body("name", isEqualTo("Widget"));
    }
}
