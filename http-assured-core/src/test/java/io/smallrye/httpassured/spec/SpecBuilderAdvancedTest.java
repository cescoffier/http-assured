package io.smallrye.httpassured.spec;

import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.assertion.BodyAssertion;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.http.Header;
import io.smallrye.httpassured.http.Headers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Advanced spec builder tests — mirrors REST Assured's ResponseSpecBuilderTest,
 * ResponseSpecBuilderExpectationsTest, and RequestSpecBuilderTest.
 */
class SpecBuilderAdvancedTest {


    @Nested
    class RequestSpecAdvanced {

        @Test
        void shouldMergeHeadersFromSpec() {
            RequestSpec spec = RequestSpec.builder()
                    .header("Authorization", "Bearer token")
                    .header("Accept", "application/json")
                    .build();

            assertEquals(2, spec.headers().size());
            assertTrue(spec.headers().hasHeader("Authorization"));
            assertTrue(spec.headers().hasHeader("Accept"));
        }

        @Test
        void shouldSetHeadersDirectly() {
            Headers headers = new Headers(List.of(
                    new Header("X-Custom", "value1"),
                    new Header("X-Another", "value2")
            ));
            RequestSpec spec = RequestSpec.builder()
                    .headers(headers)
                    .build();

            assertEquals(2, spec.headers().size());
            assertEquals("value1", spec.headers().getValue("X-Custom").orElse(null));
        }

        @Test
        void shouldAccumulateQueryParams() {
            RequestSpec spec = RequestSpec.builder()
                    .queryParam("page", "1")
                    .queryParam("page", "2") // Appends, not overrides
                    .build();

            assertEquals(1, spec.queryParams().size());
            assertEquals(java.util.List.of("1", "2"), spec.queryParams().get("page"));
        }

        @Test
        void shouldBuildEmptySpec() {
            RequestSpec spec = RequestSpec.builder().build();
            assertEquals(0, spec.headers().size());
            assertTrue(spec.queryParams().isEmpty());
            assertTrue(spec.pathParams().isEmpty());
            assertNull(spec.contentType());
            assertNull(spec.body());
        }

        @Test
        void shouldCombineHeadersQueryParamsAndBody() {
            byte[] body = "{\"key\":\"value\"}".getBytes();
            RequestSpec spec = RequestSpec.builder()
                    .header("Authorization", "Bearer token")
                    .queryParam("page", "1")
                    .pathParam("id", "42")
                    .contentType(ContentType.JSON)
                    .body(body)
                    .build();

            assertEquals(1, spec.headers().size());
            assertEquals(java.util.List.of("1"), spec.queryParams().get("page"));
            assertEquals("42", spec.pathParams().get("id"));
            assertEquals(ContentType.JSON, spec.contentType());
            assertArrayEquals(body, spec.body());
        }

        @Test
        void shouldPreserveMultiplePathParams() {
            RequestSpec spec = RequestSpec.builder()
                    .pathParam("userId", "42")
                    .pathParam("postId", "7")
                    .build();

            assertEquals(2, spec.pathParams().size());
            assertEquals("42", spec.pathParams().get("userId"));
            assertEquals("7", spec.pathParams().get("postId"));
        }
    }


    @Nested
    class ResponseSpecAdvanced {

        @Test
        void shouldBuildWithMultipleHeaderExpectations() {
            ResponseSpec spec = ResponseSpec.builder()
                    .headerEquals("Content-Type", "application/json")
                    .headerEquals("Cache-Control", "no-cache")
                    .build();

            assertEquals(2, spec.headerExpectations().size());
        }

        @Test
        void shouldBuildWithMultipleBodyExpectations() {
            ResponseSpec spec = ResponseSpec.builder()
                    .body("$.name", Assertions.isEqualTo("John"))
                    .body("$.age", Assertions.greaterThan(18))
                    .body("$.email", Assertions.isNotNull())
                    .build();

            assertEquals(3, spec.bodyExpectations().size());
        }

        @Test
        void shouldBuildWithAllExpectationTypes() {
            ResponseSpec spec = ResponseSpec.builder()
                    .statusCode(200)
                    .headerEquals("Content-Type", "application/json")
                    .headerEquals("X-Request-Id", "abc123")
                    .body("$.name", Assertions.isEqualTo("John"))
                    .body("$.items", Assertions.hasSize(3))
                    .build();

            assertEquals(200, spec.expectedStatusCode());
            assertEquals(2, spec.headerExpectations().size());
            assertEquals(2, spec.bodyExpectations().size());
        }

        @Test
        void shouldBuildMinimalSpec() {
            ResponseSpec spec = ResponseSpec.builder().build();

            assertNull(spec.expectedStatusCode());
            assertTrue(spec.headerExpectations().isEmpty());
            assertTrue(spec.bodyExpectations().isEmpty());
        }

        @Test
        void shouldBuildStatusCodeOnlySpec() {
            ResponseSpec spec = ResponseSpec.builder()
                    .statusCode(404)
                    .build();

            assertEquals(404, spec.expectedStatusCode());
            assertTrue(spec.headerExpectations().isEmpty());
            assertTrue(spec.bodyExpectations().isEmpty());
        }

        @Test
        void headerExpectationRecordShouldStoreValues() {
            ResponseSpec.HeaderExpectation he = new ResponseSpec.HeaderExpectation("Content-Type", "application/json");
            assertEquals("Content-Type", he.name());
            assertEquals("application/json", he.expectedValue());
        }

        @Test
        void bodyExpectationRecordShouldStorePath() {
            ResponseSpec.BodyExpectation<Object> be = new ResponseSpec.BodyExpectation<>("$.name", Assertions.isEqualTo("John"));
            assertEquals("$.name", be.path());
            assertNotNull(be.assertion());
        }
    }


    @Nested
    class ResponseSpecExpectationValidation {

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Test
        void bodyAssertionShouldPassForMatchingValue() {
            ResponseSpec spec = ResponseSpec.builder()
                    .body("$.name", Assertions.isEqualTo("Alice"))
                    .build();

            // Directly test the assertion using raw type to work around wildcard
            BodyAssertion assertion = spec.bodyExpectations().get(0).assertion();
            assertDoesNotThrow(() -> assertion.assertValue("Alice", "$.name"));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Test
        void bodyAssertionShouldFailForMismatchedValue() {
            ResponseSpec spec = ResponseSpec.builder()
                    .body("$.name", Assertions.isEqualTo("Alice"))
                    .build();

            BodyAssertion assertion = spec.bodyExpectations().get(0).assertion();
            assertThrows(AssertionError.class,
                    () -> assertion.assertValue("Bob", "$.name"));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Test
        void multipleBodyAssertionsShouldBeIndependent() {
            ResponseSpec spec = ResponseSpec.builder()
                    .body("$.name", Assertions.isEqualTo("Alice"))
                    .body("$.age", Assertions.greaterThan(18))
                    .build();

            assertEquals(2, spec.bodyExpectations().size());

            BodyAssertion first = spec.bodyExpectations().get(0).assertion();
            BodyAssertion second = spec.bodyExpectations().get(1).assertion();

            // First should pass
            assertDoesNotThrow(() -> first.assertValue("Alice", "$.name"));

            // Second should pass for valid
            assertDoesNotThrow(() -> second.assertValue(25, "$.age"));

            // Second should fail for invalid
            assertThrows(AssertionError.class, () -> second.assertValue(15, "$.age"));
        }
    }
}
