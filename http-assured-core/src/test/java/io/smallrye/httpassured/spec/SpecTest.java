package io.smallrye.httpassured.spec;

import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.http.ContentType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecTest {

    @Nested
    class RequestSpecTest {

        @Test
        void shouldBuildWithHeaders() {
            RequestSpec spec = RequestSpec.builder()
                    .header("Authorization", "Bearer token")
                    .header("Accept", "application/json")
                    .build();

            assertEquals(2, spec.headers().size());
            assertEquals("Bearer token", spec.headers().getValue("Authorization").orElse(null));
        }

        @Test
        void shouldBuildWithQueryParams() {
            RequestSpec spec = RequestSpec.builder()
                    .queryParam("page", "1")
                    .queryParam("size", "20")
                    .build();

            assertEquals(2, spec.queryParams().size());
            assertEquals(java.util.List.of("1"), spec.queryParams().get("page"));
            assertEquals(java.util.List.of("20"), spec.queryParams().get("size"));
        }

        @Test
        void shouldBuildWithPathParams() {
            RequestSpec spec = RequestSpec.builder()
                    .pathParam("id", "42")
                    .build();

            assertEquals("42", spec.pathParams().get("id"));
        }

        @Test
        void shouldBuildWithContentType() {
            RequestSpec spec = RequestSpec.builder()
                    .contentType(ContentType.JSON)
                    .build();

            assertEquals(ContentType.JSON, spec.contentType());
        }

        @Test
        void shouldBuildWithBody() {
            byte[] body = "test".getBytes();
            RequestSpec spec = RequestSpec.builder()
                    .body(body)
                    .build();

            assertArrayEquals(body, spec.body());
        }

        @Test
        void shouldHaveNullDefaultsForOptionalFields() {
            RequestSpec spec = RequestSpec.builder().build();
            assertNull(spec.contentType());
            assertNull(spec.body());
            assertTrue(spec.queryParams().isEmpty());
            assertTrue(spec.pathParams().isEmpty());
        }
    }

    @Nested
    class ResponseSpecTest {

        @Test
        void shouldBuildWithStatusCode() {
            ResponseSpec spec = ResponseSpec.builder()
                    .statusCode(200)
                    .build();

            assertEquals(200, spec.expectedStatusCode());
        }

        @Test
        void shouldBuildWithHeaderExpectation() {
            ResponseSpec spec = ResponseSpec.builder()
                    .headerEquals("Content-Type", "application/json")
                    .build();

            assertEquals(1, spec.headerExpectations().size());
            assertEquals("Content-Type", spec.headerExpectations().get(0).name());
            assertEquals("application/json", spec.headerExpectations().get(0).expectedValue());
        }

        @Test
        void shouldBuildWithBodyExpectation() {
            ResponseSpec spec = ResponseSpec.builder()
                    .body("$.name", Assertions.isEqualTo("John"))
                    .build();

            assertEquals(1, spec.bodyExpectations().size());
            assertEquals("$.name", spec.bodyExpectations().get(0).path());
        }

        @Test
        void shouldBuildWithMultipleExpectations() {
            ResponseSpec spec = ResponseSpec.builder()
                    .statusCode(200)
                    .headerEquals("Content-Type", "application/json")
                    .body("$.name", Assertions.isEqualTo("John"))
                    .body("$.age", Assertions.satisfies(a -> (int) a > 18))
                    .build();

            assertEquals(200, spec.expectedStatusCode());
            assertEquals(1, spec.headerExpectations().size());
            assertEquals(2, spec.bodyExpectations().size());
        }

        @Test
        void shouldHaveNullStatusCodeByDefault() {
            ResponseSpec spec = ResponseSpec.builder().build();
            assertNull(spec.expectedStatusCode());
        }
    }
}
