package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.spec.RequestSpec;
import io.smallrye.httpassured.spec.ResponseSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;

/**
 * REST Assured compatibility tests -- RequestSpec and ResponseSpec builders.
 *
 * <p>Verifies that reusable request and response specifications can be built
 * once and applied to multiple requests via {@code given().spec()} and
 * {@code then().spec()}.
 */
@WireMockTest
class SpecBuilderTest {

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
    void shouldApplyRequestSpecWithHeaders() {
        stubFor(get(urlEqualTo("/spec-header"))
                .withHeader("X-Api-Key", equalTo("secret-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"authenticated\":true}")));

        RequestSpec spec = RequestSpec.builder()
                .header("X-Api-Key", "secret-key")
                .build();

        client.given()
                .spec(spec)
                .when().get("/spec-header")
                .then()
                .statusCode(200)
                .body("authenticated", isEqualTo(true));
    }

    @Test
    void shouldApplyRequestSpecWithContentTypeAndQueryParam() {
        stubFor(get(urlPathEqualTo("/spec-query"))
                .withQueryParam("format", equalTo("verbose"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"format\":\"verbose\"}")));

        RequestSpec spec = RequestSpec.builder()
                .contentType(ContentType.JSON)
                .queryParam("format", "verbose")
                .build();

        client.given()
                .spec(spec)
                .when().get("/spec-query")
                .then()
                .statusCode(200)
                .body("format", isEqualTo("verbose"));
    }

    @Test
    void shouldApplyResponseSpecWithStatusCode() {
        stubFor(get(urlEqualTo("/spec-status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));

        ResponseSpec spec = ResponseSpec.builder()
                .statusCode(200)
                .build();

        client.when().get("/spec-status")
                .then()
                .spec(spec);
    }

    @Test
    void shouldApplyResponseSpecWithHeaderAndBody() {
        stubFor(get(urlEqualTo("/spec-body"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Request-Id", "req-42")
                        .withBody("{\"name\":\"Alice\",\"score\":95}")));

        ResponseSpec spec = ResponseSpec.builder()
                .statusCode(200)
                .headerEquals("X-Request-Id", "req-42")
                .body("name", isEqualTo("Alice"))
                .body("score", isEqualTo(95))
                .build();

        client.when().get("/spec-body")
                .then()
                .spec(spec);
    }

    @Test
    void shouldComposeRequestAndResponseSpecs() {
        stubFor(get(urlPathEqualTo("/spec-composed"))
                .withHeader("Accept", equalTo("application/json"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Total-Count", "42")
                        .withBody("{\"page\":1,\"items\":[\"a\",\"b\"]}")));

        RequestSpec requestSpec = RequestSpec.builder()
                .header("Accept", "application/json")
                .queryParam("page", "1")
                .build();

        ResponseSpec responseSpec = ResponseSpec.builder()
                .statusCode(200)
                .headerEquals("X-Total-Count", "42")
                .body("page", isEqualTo(1))
                .build();

        client.given()
                .spec(requestSpec)
                .when().get("/spec-composed")
                .then()
                .spec(responseSpec);
    }
}
