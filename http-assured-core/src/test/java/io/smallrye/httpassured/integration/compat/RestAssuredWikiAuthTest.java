package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.log.LogCapture;
import io.smallrye.httpassured.log.RequestLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REST Assured compatibility tests — Authentication.
 *
 * <p>Covers the four auth mechanisms described at
 * <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#authentication">
 * REST Assured wiki — Authentication</a>:
 * <ul>
 *   <li>Basic auth (non-preemptive alias)</li>
 *   <li>Pre-emptive Basic auth</li>
 *   <li>OAuth 2 bearer token</li>
 *   <li>OAuth 1 HMAC-SHA1</li>
 * </ul>
 *
 * <p>All stubs use WireMock's {@code withHeader} matching so the test fails at the
 * stub level if the wrong header value is sent.
 */
@WireMockTest
class RestAssuredWikiAuthTest {

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
    class BasicAuth {

        @Test
        void sendsCorrectBase64EncodedHeader() {
            String expected = "Basic " + Base64.getEncoder()
                    .encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));

            stubFor(get(urlEqualTo("/secure"))
                    .withHeader("Authorization", equalTo(expected))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().basic("user", "pass")
                    .when().get("/secure")
                    .then().statusCode(200);
        }

        @Test
        void handlesSpecialCharactersInCredentials() {
            String credentials = "user@example.com:p@$$w0rd!";
            String expected = "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            stubFor(get(urlEqualTo("/secure"))
                    .withHeader("Authorization", equalTo(expected))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().basic("user@example.com", "p@$$w0rd!")
                    .when().get("/secure")
                    .then().statusCode(200);
        }
    }

    @Nested
    class PreemptiveBasicAuth {

        @Test
        void sendsIdenticalHeaderToNonPreemptive() {
            String expected = "Basic " + Base64.getEncoder()
                    .encodeToString("admin:secret".getBytes(StandardCharsets.UTF_8));

            stubFor(get(urlEqualTo("/secure"))
                    .withHeader("Authorization", equalTo(expected))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().preemptive().basic("admin", "secret")
                    .when().get("/secure")
                    .then().statusCode(200);
        }

        @Test
        void preemptiveAndNonPreemptiveProduceSameHeader() {
            String user = "alice";
            String pass = "wonderland";
            String expected = "Basic " + Base64.getEncoder()
                    .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));

            stubFor(get(urlEqualTo("/secure"))
                    .withHeader("Authorization", equalTo(expected))
                    .willReturn(aResponse().withStatus(200)));

            // Both calls must satisfy the same stub
            client.given()
                    .auth().basic(user, pass)
                    .when().get("/secure")
                    .then().statusCode(200);

            client.given()
                    .auth().preemptive().basic(user, pass)
                    .when().get("/secure")
                    .then().statusCode(200);
        }
    }

    @Nested
    class OAuth2 {

        @Test
        void sendsBearerToken() {
            stubFor(get(urlEqualTo("/api/resource"))
                    .withHeader("Authorization", equalTo("Bearer my-access-token"))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().oauth2("my-access-token")
                    .when().get("/api/resource")
                    .then().statusCode(200);
        }

        @Test
        void bearerPrefixIsAlwaysPresent() {
            stubFor(get(urlEqualTo("/api/resource"))
                    .withHeader("Authorization", matching("^Bearer .+$"))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().oauth2("eyJhbGciOiJSUzI1NiJ9.payload.signature")
                    .when().get("/api/resource")
                    .then().statusCode(200);
        }

    }

    @Nested
    class OAuth1 {

        private static final String CONSUMER_KEY = "myConsumerKey";
        private static final String CONSUMER_SECRET = "myConsumerSecret";
        private static final String ACCESS_TOKEN = "myAccessToken";
        private static final String TOKEN_SECRET = "myTokenSecret";

        @Test
        void sendsOAuthSignedAuthorizationHeader() {
            // Stub accepts any request whose Authorization header starts with "OAuth "
            stubFor(get(urlEqualTo("/api/resource"))
                    .withHeader("Authorization", matching("^OAuth .*"))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET)
                    .when().get("/api/resource")
                    .then().statusCode(200);
        }

        @Test
        void signedHeaderContainsConsumerKey() {
            stubFor(get(urlEqualTo("/api/resource"))
                    .withHeader("Authorization", matching(".*oauth_consumer_key=\"" + CONSUMER_KEY + "\".*"))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET)
                    .when().get("/api/resource")
                    .then().statusCode(200);
        }

        @Test
        void signedHeaderContainsOAuthSignature() {
            stubFor(get(urlEqualTo("/api/resource"))
                    .withHeader("Authorization", matching(".*oauth_signature=.*"))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET)
                    .when().get("/api/resource")
                    .then().statusCode(200);
        }

        @Test
        void signedHeaderContainsOAuthToken() {
            stubFor(get(urlEqualTo("/api/resource"))
                    .withHeader("Authorization", matching(".*oauth_token=\"" + ACCESS_TOKEN + "\".*"))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET)
                    .when().get("/api/resource")
                    .then().statusCode(200);
        }
    }

    @Nested
    class LogMasking {

        private LogCapture capture;

        @BeforeEach
        void installCapture() {
            capture = LogCapture.install();
        }

        @AfterEach
        void uninstallCapture() {
            capture.uninstall();
        }

        @Test
        void basicAuth_passwordIsBlacklistedInLogs() {
            stubFor(get(urlEqualTo("/secure"))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().basic("user", "super-secret-password")
                    .log().headers()
                    .when().get("/secure")
                    .then().statusCode(200);

            assertTrue(capture.hasInfo(RequestLogger.BLACKLISTED),
                    "Authorization header must be logged as [ BLACKLISTED ]");
            assertFalse(capture.hasInfo("super-secret-password"),
                    "The actual password must not appear in the log");
            assertFalse(capture.hasInfo("Basic "),
                    "The raw Base64-encoded credentials must not appear in the log");
        }

        @Test
        void oauth2_tokenIsBlacklistedInLogs() {
            stubFor(get(urlEqualTo("/api/resource"))
                    .willReturn(aResponse().withStatus(200)));

            client.given()
                    .auth().oauth2("top-secret-bearer-token")
                    .log().headers()
                    .when().get("/api/resource")
                    .then().statusCode(200);

            assertTrue(capture.hasInfo(RequestLogger.BLACKLISTED),
                    "Authorization header must be logged as [ BLACKLISTED ]");
            assertFalse(capture.hasInfo("top-secret-bearer-token"),
                    "The actual bearer token must not appear in the log");
        }
    }
}
