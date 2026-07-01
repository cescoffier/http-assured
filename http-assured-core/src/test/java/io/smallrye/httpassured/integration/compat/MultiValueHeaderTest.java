package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REST Assured compatibility tests -- multi-value headers.
 *
 * <p>Verifies that the same header name can carry multiple values on both
 * the request and response side, matching REST Assured's
 * {@code header("name", "v1", "v2")} and
 * {@code response.headers().getValues("name")} behaviour.
 */
@WireMockTest
class MultiValueHeaderTest {

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
    void shouldSendMultipleHeadersWithSameName() {
        stubFor(get(urlEqualTo("/multi-header"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("ok")));

        client.given()
                .header("X-Custom", "val1", "val2")
                .when().get("/multi-header")
                .then()
                .statusCode(200);

        // WireMock receives multi-value headers; verify both values arrived
        verify(getRequestedFor(urlEqualTo("/multi-header"))
                .withHeader("X-Custom", containing("val1")));
        verify(getRequestedFor(urlEqualTo("/multi-header"))
                .withHeader("X-Custom", containing("val2")));
    }

    @Test
    void shouldSendMultipleHeadersWithSeparateCalls() {
        stubFor(get(urlEqualTo("/separate-header"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("ok")));

        // Two separate header() calls with the same name should both be sent
        // because Headers.with() appends rather than replaces.
        client.given()
                .header("X-Custom", "val1")
                .header("X-Custom", "val2")
                .when().get("/separate-header")
                .then()
                .statusCode(200);

        verify(getRequestedFor(urlEqualTo("/separate-header"))
                .withHeader("X-Custom", containing("val1")));
        verify(getRequestedFor(urlEqualTo("/separate-header"))
                .withHeader("X-Custom", containing("val2")));
    }


    @Test
    void shouldGetMultipleResponseHeaderValues() {
        stubFor(get(urlEqualTo("/multi-set-cookie"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Set-Cookie", "a=1")
                        .withHeader("Set-Cookie", "b=2")
                        .withBody("ok")));

        Response response = client.when().get("/multi-set-cookie");

        List<String> values = response.headers().getValues("Set-Cookie");
        assertEquals(2, values.size(), "Expected two Set-Cookie values");
        assertTrue(values.contains("a=1"), "Expected 'a=1' in Set-Cookie values");
        assertTrue(values.contains("b=2"), "Expected 'b=2' in Set-Cookie values");
    }

    @Test
    void shouldGetMultipleResponseHeaderObjects() {
        stubFor(get(urlEqualTo("/multi-header-objects"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Set-Cookie", "a=1")
                        .withHeader("Set-Cookie", "b=2")
                        .withBody("ok")));

        Response response = client.when().get("/multi-header-objects");

        List<Header> headers = response.headers().getList("Set-Cookie");
        assertEquals(2, headers.size(), "Expected two Set-Cookie Header objects");
        // All returned headers should have the name "Set-Cookie"
        assertTrue(headers.stream().allMatch(h -> h.name().equalsIgnoreCase("Set-Cookie")));
        // Values should contain both cookies
        List<String> values = headers.stream().map(Header::value).toList();
        assertTrue(values.contains("a=1"), "Expected 'a=1' in header list values");
        assertTrue(values.contains("b=2"), "Expected 'b=2' in header list values");
    }

    @Test
    void shouldGetFirstValueForMultiValueHeader() {
        stubFor(get(urlEqualTo("/multi-first"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Set-Cookie", "a=1")
                        .withHeader("Set-Cookie", "b=2")
                        .withBody("ok")));

        Response response = client.when().get("/multi-first");

        // getValue() returns the first matching header value
        String first = response.headers().getValue("Set-Cookie").orElse(null);
        assertEquals("a=1", first, "getValue() should return the first Set-Cookie value");

        // getHeader() (REST Assured compat) also returns the first value
        assertEquals("a=1", response.getHeader("Set-Cookie"));
    }
}
