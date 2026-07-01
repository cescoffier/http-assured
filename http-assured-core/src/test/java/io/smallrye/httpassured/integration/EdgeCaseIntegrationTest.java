package io.smallrye.httpassured.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.spec.RequestSpec;
import io.smallrye.httpassured.spec.ResponseSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.allMatch;
import static io.smallrye.httpassured.assertion.Assertions.anyMatch;
import static io.smallrye.httpassured.assertion.Assertions.contains;
import static io.smallrye.httpassured.assertion.Assertions.containsAll;
import static io.smallrye.httpassured.assertion.Assertions.containsString;
import static io.smallrye.httpassured.assertion.Assertions.greaterThan;
import static io.smallrye.httpassured.assertion.Assertions.hasSize;
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.isNotNull;
import static io.smallrye.httpassured.assertion.Assertions.isNull;
import static io.smallrye.httpassured.assertion.Assertions.lessThan;
import static io.smallrye.httpassured.assertion.Assertions.matchesPattern;
import static io.smallrye.httpassured.assertion.Assertions.satisfies;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for edge cases and error scenarios using WireMock stubs.
 */
@WireMockTest
class EdgeCaseIntegrationTest {

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
    class ErrorStatusCodes {

        @Test
        void shouldAssert404() {
            stubFor(get(urlEqualTo("/error/404"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\":\"Not Found\",\"message\":\"Resource not found\"}")));

            client.when()
                    .get("/error/404")
                    .then()
                    .statusCode(404)
                    .body("error", isEqualTo("Not Found"))
                    .body("message", isEqualTo("Resource not found"));
        }

        @Test
        void shouldAssert500() {
            stubFor(get(urlEqualTo("/error/500"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\":\"Internal Server Error\"}")));

            client.when()
                    .get("/error/500")
                    .then()
                    .statusCode(500)
                    .body("error", isEqualTo("Internal Server Error"));
        }

        @Test
        void shouldFailWhenStatusCodeMismatch() {
            stubFor(get(urlEqualTo("/error/404"))
                    .willReturn(aResponse().withStatus(404).withBody("{}")));

            assertThrows(AssertionError.class, () ->
                    client.when()
                            .get("/error/404")
                            .then()
                            .statusCode(200));
        }
    }

    @Nested
    class NullAndEmptyBody {

        @Test
        void shouldHandleNullJsonField() {
            stubFor(get(urlEqualTo("/null-field"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"test\",\"nickname\":null}")));

            client.when()
                    .get("/null-field")
                    .then()
                    .statusCode(200)
                    .body("name", isEqualTo("test"))
                    .body("nickname", isNull());
        }

        @Test
        void shouldHandleNonExistentField() {
            stubFor(get(urlEqualTo("/null-field"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"test\",\"nickname\":null}")));

            client.when()
                    .get("/null-field")
                    .then()
                    .statusCode(200)
                    .body("nonexistent", isNull());
        }

        @Test
        void shouldAccessEmptyBody() {
            stubFor(get(urlEqualTo("/empty"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("")));

            Response response = client.when().get("/empty");
            assertEquals(200, response.statusCode());
            assertEquals("", response.bodyAsString());
        }
    }

    @Nested
    class DeepNesting {

        @Test
        void shouldAssertDeeplyNestedValue() {
            stubFor(get(urlEqualTo("/nested/deep"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"level1\":{\"level2\":{\"level3\":{\"value\":\"deep-value\"}}}}")));

            client.when()
                    .get("/nested/deep")
                    .then()
                    .statusCode(200)
                    .body("level1.level2.level3.value", isEqualTo("deep-value"));
        }

        @Test
        void shouldExtractDeeplyNestedValue() {
            stubFor(get(urlEqualTo("/nested/deep"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"level1\":{\"level2\":{\"level3\":{\"value\":\"deep-value\"}}}}")));

            String value = client.when()
                    .get("/nested/deep")
                    .then()
                    .extract("level1.level2.level3.value");

            assertEquals("deep-value", value);
        }
    }

    @Nested
    class ArrayAssertions {

        @Test
        void shouldAssertRootArray() {
            stubFor(get(urlEqualTo("/array-root"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"id\":1,\"name\":\"first\"},{\"id\":2,\"name\":\"second\"},{\"id\":3,\"name\":\"third\"}]")));

            client.when()
                    .get("/array-root")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(3))
                    .body("[0].name", isEqualTo("first"))
                    .body("[2].id", isEqualTo(3));
        }

        @Test
        void shouldAssertLargeArray() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 20; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"id\":").append(i).append(",\"name\":\"item-").append(i).append("\",\"active\":").append(i % 2 == 0).append("}");
            }
            sb.append("]");

            stubFor(get(urlEqualTo("/large-array"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(sb.toString())));

            client.when()
                    .get("/large-array")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(20))
                    .body("[0].id", isEqualTo(0))
                    .body("[19].id", isEqualTo(19));
        }

        @Test
        void shouldFilterLargeArrayWithJsonPath() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 20; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"id\":").append(i).append(",\"active\":").append(i % 2 == 0).append("}");
            }
            sb.append("]");

            stubFor(get(urlEqualTo("/large-array"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(sb.toString())));

            client.when()
                    .get("/large-array")
                    .then()
                    .body("$[?(@.active == true)]", hasSize(10))
                    .body("$[?(@.id < 5)]", hasSize(5));
        }

        @Test
        void shouldUseAllMatchOnArray() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 20; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"id\":").append(i).append("}");
            }
            sb.append("]");

            stubFor(get(urlEqualTo("/large-array"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(sb.toString())));

            client.when()
                    .get("/large-array")
                    .then()
                    .body("$[*].id", allMatch(id -> ((Number) id).intValue() >= 0));
        }

        @Test
        void shouldUseAnyMatchOnArray() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 20; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"item-").append(i).append("\"}");
            }
            sb.append("]");

            stubFor(get(urlEqualTo("/large-array"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(sb.toString())));

            client.when()
                    .get("/large-array")
                    .then()
                    .body("$[*].name", anyMatch(name -> "item-5".equals(name)));
        }

        @Test
        void shouldUseContainsOnArray() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 20; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"item-").append(i).append("\"}");
            }
            sb.append("]");

            stubFor(get(urlEqualTo("/large-array"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(sb.toString())));

            client.when()
                    .get("/large-array")
                    .then()
                    .body("$[*].name", contains("item-0"));
        }

        @Test
        void shouldUseContainsAllOnArray() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 20; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"item-").append(i).append("\"}");
            }
            sb.append("]");

            stubFor(get(urlEqualTo("/large-array"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(sb.toString())));

            client.when()
                    .get("/large-array")
                    .then()
                    .body("$[*].name", containsAll("item-0", "item-19"));
        }
    }

    @Nested
    class JsonTypeAssertions {

        private static final String TYPES_BODY =
                "{\"string\":\"hello\",\"integer\":42,\"decimal\":3.14," +
                        "\"boolTrue\":true,\"boolFalse\":false,\"nullVal\":null," +
                        "\"array\":[1,2,3],\"nested\":{\"key\":\"val\"}}";

        private void stubTypes() {
            stubFor(get(urlEqualTo("/types"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(TYPES_BODY)));
        }

        @Test
        void shouldAssertStringType() {
            stubTypes();
            client.when().get("/types").then().body("string", isEqualTo("hello"));
        }

        @Test
        void shouldAssertIntegerType() {
            stubTypes();
            client.when().get("/types").then().body("integer", isEqualTo(42));
        }

        @Test
        void shouldAssertDecimalType() {
            stubTypes();
            client.when().get("/types").then().body("decimal", isEqualTo(3.14));
        }

        @Test
        void shouldAssertBooleanType() {
            stubTypes();
            client.when().get("/types").then()
                    .body("boolTrue", isEqualTo(true))
                    .body("boolFalse", isEqualTo(false));
        }

        @Test
        void shouldAssertNullType() {
            stubTypes();
            client.when().get("/types").then().body("nullVal", isNull());
        }

        @Test
        void shouldAssertNestedObject() {
            stubTypes();
            client.when().get("/types").then().body("nested.key", isEqualTo("val"));
        }

        @Test
        void shouldAssertArrayField() {
            stubTypes();
            client.when().get("/types").then()
                    .body("array", hasSize(3))
                    .body("array", contains(2));
        }

        @Test
        void shouldUseGreaterThanOnInteger() {
            stubTypes();
            client.when().get("/types").then().body("integer", greaterThan(40));
        }

        @Test
        void shouldUseLessThanOnDecimal() {
            stubTypes();
            client.when().get("/types").then().body("decimal", lessThan(4.0));
        }

        @Test
        void shouldUseSatisfiesWithCustomPredicate() {
            stubTypes();
            client.when().get("/types").then()
                    .body("string", satisfies(s -> ((String) s).length() == 5));
        }

        @Test
        void shouldUseIsNotNull() {
            stubTypes();
            client.when().get("/types").then()
                    .body("string", isNotNull())
                    .body("integer", isNotNull())
                    .body("nested", isNotNull());
        }

        @Test
        void shouldUseIsNotEqualTo() {
            stubTypes();
            client.when().get("/types").then()
                    .body("string", isNotEqualTo("world"))
                    .body("integer", isNotEqualTo(0));
        }

        @Test
        void shouldUseContainsStringOnStringValue() {
            stubTypes();
            client.when().get("/types").then().body("string", containsString("ell"));
        }

        @Test
        void shouldUseMatchesPatternOnStringValue() {
            stubTypes();
            client.when().get("/types").then().body("string", matchesPattern("h.*o"));
        }
    }

    @Nested
    class ContentTypeWithCharset {

        @Test
        void shouldMatchContentTypeIgnoringCharset() {
            stubFor(get(urlEqualTo("/content-type-charset"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json; charset=UTF-8")
                            .withBody("{\"charset\":\"utf8\"}")));

            client.when()
                    .get("/content-type-charset")
                    .then()
                    .statusCode(200)
                    .contentType("application/json");
        }

        @Test
        void shouldAssertFullContentTypeHeader() {
            stubFor(get(urlEqualTo("/content-type-charset"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json; charset=UTF-8")
                            .withBody("{\"charset\":\"utf8\"}")));

            client.when()
                    .get("/content-type-charset")
                    .then()
                    .header("Content-Type", "application/json; charset=UTF-8");
        }
    }

    @Nested
    class MultiplePathParams {

        @Test
        void shouldSubstituteMultiplePositionalPathParams() {
            stubFor(get(urlEqualTo("/repos/smallrye/http-assured/issues/42"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"org\":\"smallrye\",\"repo\":\"http-assured\",\"number\":42}")));

            client.given()
                    .when()
                    .get("/repos/{org}/{repo}/issues/{number}", "smallrye", "http-assured", 42)
                    .then()
                    .statusCode(200)
                    .body("org", isEqualTo("smallrye"))
                    .body("repo", isEqualTo("http-assured"))
                    .body("number", isEqualTo(42));
        }

        @Test
        void shouldSubstituteNamedPathParams() {
            stubFor(get(urlEqualTo("/repos/smallrye/http-assured/issues/7"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"org\":\"smallrye\",\"repo\":\"http-assured\",\"number\":7}")));

            client.given()
                    .pathParam("org", "smallrye")
                    .pathParam("repo", "http-assured")
                    .pathParam("number", "7")
                    .when()
                    .get("/repos/{org}/{repo}/issues/{number}")
                    .then()
                    .statusCode(200)
                    .body("org", isEqualTo("smallrye"))
                    .body("repo", isEqualTo("http-assured"))
                    .body("number", isEqualTo(7));
        }
    }

    @Nested
    class RequestBodyEcho {

        @Test
        void shouldSendStringBody() {
            stubFor(post(urlEqualTo("/echo"))
                    .withHeader("Content-Type", containing("application/json"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"receivedBody\":\"{\\\"key\\\":\\\"value\\\"}\",\"receivedContentType\":\"application/json\"}")));

            client.given()
                    .contentType(ContentType.JSON)
                    .body("{\"key\":\"value\"}")
                    .when()
                    .post("/echo")
                    .then()
                    .statusCode(200)
                    .body("receivedBody", containsString("key"))
                    .body("receivedContentType", isEqualTo("application/json"));
        }

        @Test
        void shouldAutoSetContentTypeForObjectBody() {
            stubFor(post(urlEqualTo("/echo"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"receivedContentType\":\"application/json\"}")));

            var obj = new TestPayload("hello", 42);
            client.given()
                    .body(obj)
                    .when()
                    .post("/echo")
                    .then()
                    .statusCode(200)
                    .body("receivedContentType", isEqualTo("application/json"));
        }
    }

    @Nested
    class SpecIntegration {

        @Test
        void shouldApplyResponseSpecInIntegration() {
            stubFor(get(urlEqualTo("/content-type-charset"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json; charset=UTF-8")
                            .withBody("{\"charset\":\"utf8\"}")));

            ResponseSpec okJson = ResponseSpec.builder()
                    .statusCode(200)
                    .headerEquals("Content-Type", "application/json; charset=UTF-8")
                    .build();

            client.when()
                    .get("/content-type-charset")
                    .then()
                    .spec(okJson)
                    .body("charset", isEqualTo("utf8"));
        }

        @Test
        void shouldApplyRequestSpecInIntegration() {
            stubFor(get(urlEqualTo("/repos/test-org/test-repo/issues/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"org\":\"test-org\",\"repo\":\"test-repo\",\"number\":1}")));

            RequestSpec pathSpec = RequestSpec.builder()
                    .pathParam("org", "test-org")
                    .pathParam("repo", "test-repo")
                    .pathParam("number", "1")
                    .build();

            client.given()
                    .spec(pathSpec)
                    .when()
                    .get("/repos/{org}/{repo}/issues/{number}")
                    .then()
                    .statusCode(200)
                    .body("org", isEqualTo("test-org"));
        }

        @Test
        void shouldCombineSpecWithInlineConfig() {
            stubFor(post(urlEqualTo("/echo"))
                    .withHeader("Content-Type", containing("application/json"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"receivedContentType\":\"application/json\"}")));

            RequestSpec baseSpec = RequestSpec.builder()
                    .contentType(ContentType.JSON)
                    .build();

            client.given()
                    .spec(baseSpec)
                    .body("{\"extra\":\"data\"}")
                    .when()
                    .post("/echo")
                    .then()
                    .statusCode(200)
                    .body("receivedContentType", isEqualTo("application/json"));
        }
    }

    @Nested
    class HeaderAssertions {

        @Test
        void shouldAssertHeaderExists() {
            stubFor(get(urlEqualTo("/multi-header"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Custom", "value1")
                            .withHeader("X-Another", "value2")
                            .withBody("{\"ok\":true}")));

            client.when()
                    .get("/multi-header")
                    .then()
                    .headerExists("X-Custom")
                    .headerExists("X-Another");
        }

        @Test
        void shouldAssertHeaderContains() {
            stubFor(get(urlEqualTo("/multi-header"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"ok\":true}")));

            client.when()
                    .get("/multi-header")
                    .then()
                    .headerContains("Content-Type", "json");
        }

        @Test
        void shouldFailWhenHeaderMissing() {
            stubFor(get(urlEqualTo("/multi-header"))
                    .willReturn(aResponse().withStatus(200).withBody("{}")));

            assertThrows(AssertionError.class, () ->
                    client.when()
                            .get("/multi-header")
                            .then()
                            .header("X-Nonexistent", "value"));
        }

        @Test
        void shouldFailWhenHeaderValueMismatch() {
            stubFor(get(urlEqualTo("/multi-header"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("X-Custom", "value1")
                            .withBody("{}")));

            assertThrows(AssertionError.class, () ->
                    client.when()
                            .get("/multi-header")
                            .then()
                            .header("X-Custom", "wrong-value"));
        }
    }

    @Nested
    class BodyStringAssertions {

        @Test
        void shouldAssertBodyContains() {
            stubFor(get(urlEqualTo("/types"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"string\":\"hello\",\"integer\":42}")));

            client.when()
                    .get("/types")
                    .then()
                    .bodyContains("\"string\":\"hello\"");
        }

        @Test
        void shouldFailBodyEqualsOnMismatch() {
            stubFor(get(urlEqualTo("/types"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"string\":\"hello\"}")));

            assertThrows(AssertionError.class, () ->
                    client.when()
                            .get("/types")
                            .then()
                            .bodyEquals("not the body"));
        }
    }

    @Nested
    class ResponseAccess {

        @Test
        void shouldAccessStatusCode() {
            stubFor(get(urlEqualTo("/types"))
                    .willReturn(aResponse().withStatus(200).withBody("{\"string\":\"hello\"}")));

            Response response = client.when().get("/types");
            assertEquals(200, response.statusCode());
        }

        @Test
        void shouldAccessHeaders() {
            stubFor(get(urlEqualTo("/multi-header"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("X-Custom", "value1")
                            .withBody("{}")));

            Response response = client.when().get("/multi-header");
            assertTrue(response.headers().hasHeader("X-Custom"));
            assertEquals("value1", response.headers().getValue("X-Custom").orElse(null));
        }

        @Test
        void shouldAccessBodyAsString() {
            stubFor(get(urlEqualTo("/types"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"string\":\"hello\"}")));

            Response response = client.when().get("/types");
            assertNotNull(response.bodyAsString());
            assertTrue(response.bodyAsString().contains("hello"));
        }

        @Test
        void shouldAccessBodyAsBytes() {
            stubFor(get(urlEqualTo("/types"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"string\":\"hello\"}")));

            Response response = client.when().get("/types");
            assertNotNull(response.bodyAsBytes());
            assertTrue(response.bodyAsBytes().length > 0);
        }

        @Test
        void shouldDeserializeBody() {
            stubFor(get(urlEqualTo("/null-field"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"test\",\"nickname\":null}")));

            TestPayload payload = client.when()
                    .get("/null-field")
                    .bodyAs(TestPayload.class);
            assertEquals("test", payload.name);
        }

        @Test
        void shouldExtractAndDeserialize() {
            stubFor(get(urlEqualTo("/null-field"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"name\":\"test\",\"nickname\":null}")));

            TestPayload payload = client.when()
                    .get("/null-field")
                    .then()
                    .statusCode(200)
                    .extractAs(TestPayload.class);
            assertEquals("test", payload.name);
        }
    }

    public static class TestPayload {
        public String name;
        public int id;
        public String nickname;

        public TestPayload() {
        }

        public TestPayload(String name, int id) {
            this.name = name;
            this.id = id;
        }
    }
}
