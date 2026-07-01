package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.http.Cookie;
import io.smallrye.httpassured.http.Cookies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * REST Assured compatibility tests — Cookies section of the REST Assured wiki.
 *
 * <p>Mirrors the code samples from
 * <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#cookies">
 * REST Assured Wiki – Cookies</a>.
 *
 * <p>The REST Assured Hamcrest-based overload
 * {@code .cookies("n1","v1","n2", containsString("v2"))} is covered here using
 * the JUnit 5 equivalent: {@code .cookies("n1","v1","n2", Assertions.containsString("v2"))}.
 */
@WireMockTest
class RestAssuredWikiCookieTest {

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
    class SendingCookies {

        /**
         * Wiki sample:
         * <pre>given().cookie("username", "John").when().get("/cookie").then().body(equalTo("username"));</pre>
         */
        @Test
        void shouldSendSimpleCookieNameValue() {
            stubFor(get(urlEqualTo("/cookie"))
                    .withHeader("Cookie", equalTo("username=John"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("username")));

            client.given()
                    .cookie("username", "John")
                    .when().get("/cookie")
                    .then()
                    .statusCode(200)
                    .bodyEquals("username");
        }

        /**
         * Wiki sample:
         * <pre>given().cookie("cookieName", "value1", "value2"). ..</pre>
         * Creates <em>two</em> cookies: {@code cookieName=value1} and {@code cookieName=value2}.
         */
        @Test
        void shouldSendMultiValueCookie() {
            stubFor(get(urlEqualTo("/cookie"))
                    .withHeader("Cookie", containing("cookieName=value1"))
                    .withHeader("Cookie", containing("cookieName=value2"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("ok")));

            client.given()
                    .cookie("cookieName", "value1", "value2")
                    .when().get("/cookie")
                    .then()
                    .statusCode(200);
        }

        /**
         * Wiki sample:
         * <pre>
         * Cookie someCookie = new Cookie.Builder("some_cookie", "some_value")
         *     .secure(true).comment("some comment").build();
         * given().cookie(someCookie).when().get("/cookie").then().assertThat().body(equalTo("x"));
         * </pre>
         */
        @Test
        void shouldSendDetailedCookieViaBuilder() {
            Cookie someCookie = new Cookie.Builder("some_cookie", "some_value")
                    .secure(true)
                    .comment("some comment")
                    .build();

            stubFor(get(urlEqualTo("/cookie"))
                    .withHeader("Cookie", equalTo("some_cookie=some_value"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("x")));

            client.given()
                    .cookie(someCookie)
                    .when().get("/cookie")
                    .then()
                    .statusCode(200)
                    .bodyEquals("x");
        }

        /**
         * Wiki sample:
         * <pre>
         * Cookie cookie1 = new Cookie.Builder("username", "John").comment("comment 1").build();
         * Cookie cookie2 = new Cookie.Builder("token", "1234").comment("comment 2").build();
         * Cookies cookies = new Cookies(cookie1, cookie2);
         * given().cookies(cookies).when().get("/cookie").then().body(equalTo("username, token"));
         * </pre>
         */
        @Test
        void shouldSendCookiesCollection() {
            Cookie cookie1 = new Cookie.Builder("username", "John")
                    .comment("comment 1")
                    .build();
            Cookie cookie2 = new Cookie.Builder("token", "1234")
                    .comment("comment 2")
                    .build();
            Cookies cookies = new Cookies(cookie1, cookie2);

            stubFor(get(urlEqualTo("/cookie"))
                    .withHeader("Cookie", containing("username=John"))
                    .withHeader("Cookie", containing("token=1234"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("username, token")));

            client.given()
                    .cookies(cookies)
                    .when().get("/cookie")
                    .then()
                    .statusCode(200)
                    .bodyEquals("username, token");
        }
    }

    @Nested
    class AssertingCookies {

        /**
         * Wiki sample:
         * <pre>get("/x").then().assertThat().cookie("cookieName", "cookieValue"). ..</pre>
         */
        @Test
        void shouldAssertSingleResponseCookie() {
            stubFor(get(urlEqualTo("/x"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Set-Cookie", "cookieName=cookieValue; Path=/")));

            client.given()
                    .when().get("/x")
                    .then()
                    .cookie("cookieName", "cookieValue");
        }

        /**
         * Wiki sample:
         * <pre>get("/x").then().assertThat().cookies("cookieName1", "cookieValue1", "cookieName2", "cookieValue2"). ..</pre>
         */
        @Test
        void shouldAssertMultipleResponseCookiesViaVarargs() {
            stubFor(get(urlEqualTo("/x"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Set-Cookie", "cookieName1=cookieValue1; Path=/")
                            .withHeader("Set-Cookie", "cookieName2=cookieValue2; HttpOnly")));

            client.given()
                    .when().get("/x")
                    .then()
                    .cookies("cookieName1", "cookieValue1",
                             "cookieName2", "cookieValue2");
        }

        /**
         * Wiki sample (JUnit 5 equivalent):
         * <pre>
         * // REST Assured (Hamcrest)
         * get("/x").then().assertThat().cookies("cookieName1", "cookieValue1", "cookieName2", containsString("Value2"));
         *
         * // http-assured (JUnit 5)
         * get("/x").then().cookies("cookieName1", "cookieValue1", "cookieName2", Assertions.containsString("Value2"));
         * </pre>
         */
        @Test
        void shouldAssertCookiesWithMixedStringAndAssertion() {
            stubFor(get(urlEqualTo("/x"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Set-Cookie", "cookieName1=cookieValue1; Path=/")
                            .withHeader("Set-Cookie", "cookieName2=cookieValue2; HttpOnly")));

            client.given()
                    .when().get("/x")
                    .then()
                    .cookies("cookieName1", "cookieValue1",
                             "cookieName2", Assertions.containsString("Value2"));
        }

        /**
         * {@code cookie(name, BodyAssertion)} single-cookie assertion overload.
         */
        @Test
        void shouldAssertCookieWithBodyAssertion() {
            stubFor(get(urlEqualTo("/x"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Set-Cookie", "token=Bearer-abc123; HttpOnly")));

            client.given()
                    .when().get("/x")
                    .then()
                    .cookie("token", Assertions.containsString("abc123"));
        }
    }
}
