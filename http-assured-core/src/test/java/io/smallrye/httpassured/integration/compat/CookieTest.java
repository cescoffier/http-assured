package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.http.Cookie;
import io.smallrye.httpassured.http.Cookies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * REST Assured compatibility tests -- additional cookie scenarios.
 *
 * <p>Complements {@link RestAssuredWikiCookieTest} (wiki-based cookie examples)
 * and the cookie section of {@link HeadersParametersTest} (Baeldung article).
 * Each test here covers a scenario not exercised by those two suites:
 * <ul>
 *   <li>Sending a cookie with three values via the multi-value overload</li>
 *   <li>Asserting a response cookie with {@code matchesPattern}</li>
 *   <li>Asserting three response cookies in a single {@code cookies()} call</li>
 *   <li>Sending cookies built from a {@code Cookies(List)} collection via POST</li>
 * </ul>
 */
@WireMockTest
class CookieTest {

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

    // Sending cookies

    /**
     * The multi-value cookie overload {@code cookie("name", "v1", "v2", "v3")}
     * sends three separate {@code name=value} pairs in the Cookie header.
     *
     * <p>Differs from {@link RestAssuredWikiCookieTest#SendingCookies} which
     * tests only two values.
     */
    @Test
    void shouldSendMultiValueCookie() {
        stubFor(get(urlEqualTo("/prefs"))
                .withHeader("Cookie", containing("lang=en"))
                .withHeader("Cookie", containing("lang=fr"))
                .withHeader("Cookie", containing("lang=de"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accepted\":[\"en\",\"fr\",\"de\"]}")));

        client.given()
                .cookie("lang", "en", "fr", "de")
                .when().get("/prefs")
                .then()
                .statusCode(200)
                .bodyContains("en")
                .bodyContains("fr")
                .bodyContains("de");
    }

    /**
     * Asserts a response cookie value using {@code matchesPattern} --
     * a regex-based {@link io.smallrye.httpassured.assertion.BodyAssertion}.
     *
     * <p>Differs from {@link RestAssuredWikiCookieTest.AssertingCookies} which
     * uses {@code containsString} for its BodyAssertion tests.
     */
    @Test
    void shouldAssertCookieWithMatchesPattern() {
        stubFor(get(urlEqualTo("/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Set-Cookie", "session=sess-abc-42; Path=/; HttpOnly")));

        client.given()
                .when().get("/login")
                .then()
                .statusCode(200)
                .cookie("session", Assertions.matchesPattern("sess-[a-z]+-\\d+"));
    }

    // Asserting cookies

    /**
     * Asserts three response cookies in a single {@code cookies()} call using
     * alternating name/value pairs.
     *
     * <p>Differs from {@link RestAssuredWikiCookieTest.AssertingCookies} which
     * tests only two cookie pairs in its varargs call.
     */
    @Test
    void shouldAssertMultipleCookiesInOneLine() {
        stubFor(get(urlEqualTo("/dashboard"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Set-Cookie", "theme=dark; Path=/")
                        .withHeader("Set-Cookie", "locale=en_US; Path=/")
                        .withHeader("Set-Cookie", "tz=UTC; Path=/")));

        client.given()
                .when().get("/dashboard")
                .then()
                .statusCode(200)
                .cookies("theme", "dark",
                         "locale", "en_US",
                         "tz", "UTC");
    }

    /**
     * Sends cookies created from a {@link Cookies} collection built via the
     * {@code Cookies(List)} constructor, using a POST request.
     *
     * <p>Differs from {@link RestAssuredWikiCookieTest.SendingCookies} which
     * uses the varargs constructor and a GET request.
     */
    @Test
    void shouldSendCookiesFromCookiesCollection() {
        Cookie csrfToken = new Cookie.Builder("csrf", "tok-98765").build();
        Cookie preference = new Cookie.Builder("color", "blue").build();
        Cookie region = new Cookie.Builder("region", "eu-west-1").build();

        Cookies cookies = new Cookies(List.of(csrfToken, preference, region));

        stubFor(post(urlEqualTo("/settings"))
                .withHeader("Cookie", containing("csrf=tok-98765"))
                .withHeader("Cookie", containing("color=blue"))
                .withHeader("Cookie", containing("region=eu-west-1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("saved")));

        client.given()
                .cookies(cookies)
                .when().post("/settings")
                .then()
                .statusCode(200)
                .bodyEquals("saved");
    }
}
