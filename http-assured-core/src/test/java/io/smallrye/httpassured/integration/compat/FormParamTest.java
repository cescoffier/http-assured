package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * REST Assured compatibility tests — form parameter operations.
 *
 * <p>Tests {@code application/x-www-form-urlencoded} form parameter support
 * including single params, multiple params, map-based params, varargs pairs,
 * URL encoding, automatic Content-Type detection, and usage with PUT/PATCH.
 */
@WireMockTest
class FormParamTest {

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
    void shouldSendSingleFormParam() {
        stubFor(post(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .formParam("name", "value")
                .when().post("/form")
                .then()
                .statusCode(200);

        verify(postRequestedFor(urlEqualTo("/form"))
                .withRequestBody(equalTo("name=value")));
    }

    @Test
    void shouldSendMultipleFormParams() {
        stubFor(post(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .formParam("username", "john")
                .formParam("password", "secret")
                .when().post("/form")
                .then()
                .statusCode(200);

        verify(postRequestedFor(urlEqualTo("/form"))
                .withRequestBody(containing("username=john"))
                .withRequestBody(containing("password=secret")));
    }

    @Test
    void shouldSendFormParamsFromMap() {
        stubFor(post(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        // Use LinkedHashMap for deterministic ordering
        Map<String, String> params = new LinkedHashMap<>();
        params.put("city", "Paris");
        params.put("country", "France");

        client.given()
                .formParams(params)
                .when().post("/form")
                .then()
                .statusCode(200);

        verify(postRequestedFor(urlEqualTo("/form"))
                .withRequestBody(containing("city=Paris"))
                .withRequestBody(containing("country=France")));
    }

    @Test
    void shouldSendFormParamsFromVarargs() {
        stubFor(post(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .formParams("k1", "v1", "k2", "v2")
                .when().post("/form")
                .then()
                .statusCode(200);

        verify(postRequestedFor(urlEqualTo("/form"))
                .withRequestBody(containing("k1=v1"))
                .withRequestBody(containing("k2=v2")));
    }

    @Test
    void shouldAutoSetContentType() {
        stubFor(post(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .formParam("key", "val")
                .when().post("/form")
                .then()
                .statusCode(200);

        verify(postRequestedFor(urlEqualTo("/form"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded")));
    }

    @Test
    void shouldUrlEncodeFormParams() {
        stubFor(post(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .formParam("a&b", "c d")
                .when().post("/form")
                .then()
                .statusCode(200);

        // a&b -> a%26b, c d -> c+d (URLEncoder uses + for spaces)
        verify(postRequestedFor(urlEqualTo("/form"))
                .withRequestBody(equalTo("a%26b=c+d")));
    }

    @Test
    void shouldWorkWithPut() {
        stubFor(put(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .formParam("action", "update")
                .when().put("/form")
                .then()
                .statusCode(200);

        verify(putRequestedFor(urlEqualTo("/form"))
                .withRequestBody(equalTo("action=update"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded")));
    }

    @Test
    void shouldWorkWithPatch() {
        stubFor(patch(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .formParam("field", "patched")
                .when().patch("/form")
                .then()
                .statusCode(200);

        verify(patchRequestedFor(urlEqualTo("/form"))
                .withRequestBody(equalTo("field=patched"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded")));
    }

    @Test
    void shouldRejectBodyAndFormParamCombination() {
        stubFor(post(urlEqualTo("/form"))
                .willReturn(aResponse().withStatus(200)));

        assertThrows(IllegalStateException.class, () ->
                client.given()
                        .body("some body")
                        .formParam("key", "val")
                        .when().post("/form"));
    }
}
