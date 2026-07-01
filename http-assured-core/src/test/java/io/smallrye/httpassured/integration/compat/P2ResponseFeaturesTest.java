package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2 compat tests for Response features:
 * prettyPrint, peek, asPrettyString, statusLine, time, detailedCookie, extract chaining.
 */
@WireMockTest
class P2ResponseFeaturesTest {

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

    private String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return capture.toString().trim();
    }

    @Nested
    class PrettyPrintAndPeek {

        @Test
        void prettyPrintShouldFormatJsonAndReturnThis() {
            stubFor(get(urlEqualTo("/json"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"Alice\",\"age\":30}")));

            Response response = client.when().get("/json");
            String output = captureStdout(() -> {
                Response returned = response.prettyPrint();
                assertSame(response, returned);
            });
            assertTrue(output.contains("\"name\""), "Output should contain key 'name'");
            assertTrue(output.contains("Alice"), "Output should contain value 'Alice'");
            assertTrue(output.contains("\n"), "Output should be multi-line (pretty-printed)");
        }

        @Test
        void peekShouldPrintRawBodyAndReturnThis() {
            stubFor(get(urlEqualTo("/raw"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"compact\":true}")));

            Response response = client.when().get("/raw");
            String output = captureStdout(() -> {
                Response returned = response.peek();
                assertSame(response, returned);
            });
            assertEquals("{\"compact\":true}", output);
        }

        @Test
        void prettyPeekShouldFormatJsonAndReturnThis() {
            stubFor(get(urlEqualTo("/peek"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"key\":\"val\"}")));

            Response response = client.when().get("/peek");
            String output = captureStdout(() -> {
                Response returned = response.prettyPeek();
                assertSame(response, returned);
            });
            assertTrue(output.contains("\"key\""));
            assertTrue(output.contains("\n"), "prettyPeek should produce multi-line output");
        }
    }

    @Nested
    class AsPrettyString {

        @Test
        void shouldReturnPrettyPrintedJsonString() {
            stubFor(get(urlEqualTo("/pretty"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":1,\"name\":\"Bob\"}")));

            Response response = client.when().get("/pretty");

            PrintStream original = System.out;
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setOut(new PrintStream(capture));
            String pretty;
            try {
                pretty = response.asPrettyString();
            } finally {
                System.setOut(original);
            }

            assertTrue(pretty.contains("\"id\""));
            assertTrue(pretty.contains("\"name\""));
            assertTrue(pretty.contains("\n"), "Should be multi-line");
            assertEquals(0, capture.size(), "asPrettyString should not write to stdout");
        }
    }

    @Nested
    class StatusLine {

        @Test
        void shouldReturnStatusCodeAndMessage() {
            stubFor(get(urlEqualTo("/ok"))
                    .willReturn(aResponse().withStatus(200)));

            Response response = client.when().get("/ok");
            assertEquals("200 OK", response.statusLine());
        }

        @Test
        void shouldReturnStatusLineForErrorResponse() {
            stubFor(get(urlEqualTo("/notfound"))
                    .willReturn(aResponse().withStatus(404)));

            Response response = client.when().get("/notfound");
            assertEquals("404 Not Found", response.statusLine());
        }
    }

    @Nested
    class ResponseTime {

        @Test
        void timeShouldReturnNonNegativeMilliseconds() {
            stubFor(get(urlEqualTo("/time"))
                    .willReturn(aResponse().withStatus(200)));

            Response response = client.when().get("/time");
            assertTrue(response.time() >= 0, "Response time should be non-negative");
        }

        @Test
        void timeInShouldConvertToRequestedUnit() {
            stubFor(get(urlEqualTo("/time2"))
                    .willReturn(aResponse().withStatus(200)));

            Response response = client.when().get("/time2");
            long ms = response.time();
            long micros = response.timeIn(TimeUnit.MICROSECONDS);
            assertTrue(micros >= ms, "Microseconds should be >= milliseconds");
        }
    }

    @Nested
    class DetailedCookie {

        @Test
        void shouldParseFullCookieAttributes() {
            stubFor(get(urlEqualTo("/cookie"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Set-Cookie",
                                    "session=abc123; Domain=example.com; Path=/; Max-Age=3600; Secure; HttpOnly; SameSite=Strict")));

            Response response = client.when().get("/cookie");
            Cookie cookie = response.detailedCookie("session");

            assertNotNull(cookie);
            assertEquals("session", cookie.name());
            assertEquals("abc123", cookie.value());
            assertEquals("example.com", cookie.domain());
            assertEquals("/", cookie.path());
            assertEquals(3600, cookie.maxAge());
            assertTrue(cookie.isSecured());
            assertTrue(cookie.isHttpOnly());
            assertEquals("Strict", cookie.sameSite());
        }

        @Test
        void shouldReturnNullForMissingCookie() {
            stubFor(get(urlEqualTo("/nocookie"))
                    .willReturn(aResponse().withStatus(200)));

            Response response = client.when().get("/nocookie");
            assertNull(response.detailedCookie("nonexistent"));
        }

        @Test
        void shouldDefaultMaxAgeToMinusOne() {
            stubFor(get(urlEqualTo("/simple-cookie"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Set-Cookie", "token=xyz")));

            Response response = client.when().get("/simple-cookie");
            Cookie cookie = response.detailedCookie("token");

            assertNotNull(cookie);
            assertEquals("token", cookie.name());
            assertEquals("xyz", cookie.value());
            assertEquals(-1, cookie.maxAge());
        }
    }

    @Nested
    class ExtractAfterAssertions {

        @Test
        void shouldExtractAfterStatusCodeAndBodyAssertions() {
            stubFor(get(urlEqualTo("/extract"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":42,\"name\":\"Widget\"}")));

            int id = client.when().get("/extract")
                    .then()
                    .statusCode(200)
                    .body("name", isEqualTo("Widget"))
                    .extract("id");

            assertEquals(42, id);
        }
    }
}
