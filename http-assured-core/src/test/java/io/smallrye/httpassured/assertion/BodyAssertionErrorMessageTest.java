package io.smallrye.httpassured.assertion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for assertion error messages — mirrors REST Assured's BodyMatcherTest.
 * Verifies that assertion failures produce meaningful, actionable error messages.
 */
class BodyAssertionErrorMessageTest {

    @Nested
    class IsEqualToMessages {

        @Test
        void shouldIncludePathAndValuesInErrorMessage() {
            BodyAssertion<Object> assertion = Assertions.isEqualTo("expected");
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue("actual", "$.name"));
            assertTrue(error.getMessage().contains("$.name"),
                    "Error should mention the path");
        }

        @Test
        void shouldReportIntegerMismatch() {
            BodyAssertion<Object> assertion = Assertions.isEqualTo(42);
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(99, "$.age"));
            assertTrue(error.getMessage().contains("$.age"));
        }

        @Test
        void shouldPassWhenValuesMatch() {
            BodyAssertion<Object> assertion = Assertions.isEqualTo("hello");
            assertDoesNotThrow(() -> assertion.assertValue("hello", "$.greeting"));
        }

        @Test
        void shouldPassWhenIntegerValuesMatch() {
            BodyAssertion<Object> assertion = Assertions.isEqualTo(42);
            assertDoesNotThrow(() -> assertion.assertValue(42, "$.count"));
        }
    }

    @Nested
    class IsNotEqualToMessages {

        @Test
        void shouldFailWhenValuesAreEqual() {
            BodyAssertion<Object> assertion = Assertions.isNotEqualTo("same");
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue("same", "$.field"));
            assertTrue(error.getMessage().contains("$.field"));
        }

        @Test
        void shouldPassWhenValuesDiffer() {
            BodyAssertion<Object> assertion = Assertions.isNotEqualTo("a");
            assertDoesNotThrow(() -> assertion.assertValue("b", "$.field"));
        }
    }

    @Nested
    class IsNullMessages {

        @Test
        void shouldFailWhenValueIsNotNull() {
            BodyAssertion<Object> assertion = Assertions.isNull();
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue("something", "$.field"));
            assertTrue(error.getMessage().contains("$.field"));
            assertTrue(error.getMessage().contains("null"));
        }

        @Test
        void shouldPassWhenValueIsNull() {
            BodyAssertion<Object> assertion = Assertions.isNull();
            assertDoesNotThrow(() -> assertion.assertValue(null, "$.field"));
        }
    }

    @Nested
    class IsNotNullMessages {

        @Test
        void shouldFailWhenValueIsNull() {
            BodyAssertion<Object> assertion = Assertions.isNotNull();
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(null, "$.field"));
            assertTrue(error.getMessage().contains("$.field"));
        }

        @Test
        void shouldPassWhenValueIsNotNull() {
            BodyAssertion<Object> assertion = Assertions.isNotNull();
            assertDoesNotThrow(() -> assertion.assertValue("value", "$.field"));
        }
    }

    @Nested
    class SatisfiesMessages {

        @Test
        void shouldFailWithPredicateMessage() {
            BodyAssertion<Integer> assertion = Assertions.satisfies(v -> v > 100);
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(50, "$.score"));
            assertTrue(error.getMessage().contains("$.score"));
            assertTrue(error.getMessage().contains("predicate"));
        }
    }

    @Nested
    class HasSizeMessages {

        @Test
        void shouldFailWhenSizeMismatch() {
            BodyAssertion<Object> assertion = Assertions.hasSize(5);
            java.util.List<String> list = java.util.List.of("a", "b", "c");
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(list, "$.items"));
            assertTrue(error.getMessage().contains("$.items"));
            assertTrue(error.getMessage().contains("5"));
        }

        @Test
        void shouldFailWhenValueIsNull() {
            BodyAssertion<Object> assertion = Assertions.hasSize(3);
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(null, "$.items"));
            assertTrue(error.getMessage().contains("null"));
        }

        @Test
        void shouldFailWhenValueIsNotCollection() {
            BodyAssertion<Object> assertion = Assertions.hasSize(3);
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue("not-a-list", "$.items"));
            assertTrue(error.getMessage().contains("$.items"));
        }
    }

    @Nested
    class GreaterThanMessages {

        @Test
        void shouldFailWhenNotGreater() {
            BodyAssertion<Integer> assertion = Assertions.greaterThan(10);
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(5, "$.value"));
            assertTrue(error.getMessage().contains("$.value"));
            assertTrue(error.getMessage().contains("10"));
        }

        @Test
        void shouldFailWhenEqual() {
            BodyAssertion<Integer> assertion = Assertions.greaterThan(10);
            assertThrows(AssertionError.class,
                    () -> assertion.assertValue(10, "$.value"));
        }

        @Test
        void shouldPassWhenGreater() {
            BodyAssertion<Integer> assertion = Assertions.greaterThan(10);
            assertDoesNotThrow(() -> assertion.assertValue(15, "$.value"));
        }
    }

    @Nested
    class LessThanMessages {

        @Test
        void shouldFailWhenNotLess() {
            BodyAssertion<Integer> assertion = Assertions.lessThan(10);
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(15, "$.value"));
            assertTrue(error.getMessage().contains("$.value"));
        }

        @Test
        void shouldPassWhenLess() {
            BodyAssertion<Integer> assertion = Assertions.lessThan(10);
            assertDoesNotThrow(() -> assertion.assertValue(5, "$.value"));
        }
    }

    @Nested
    class ContainsMessages {

        @Test
        void shouldFailWhenNotContained() {
            BodyAssertion<Object> assertion = Assertions.contains("missing");
            java.util.List<String> list = java.util.List.of("a", "b", "c");
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(list, "$.items"));
            assertTrue(error.getMessage().contains("$.items"));
            assertTrue(error.getMessage().contains("missing"));
        }
    }

    @Nested
    class ContainsStringMessages {

        @Test
        void shouldFailWhenStringDoesNotContain() {
            BodyAssertion<String> assertion = Assertions.containsString("xyz");
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue("hello world", "$.text"));
            assertTrue(error.getMessage().contains("$.text"));
            assertTrue(error.getMessage().contains("xyz"));
        }
    }

    @Nested
    class MatchesPatternMessages {

        @Test
        void shouldFailWhenPatternDoesNotMatch() {
            BodyAssertion<String> assertion = Assertions.matchesPattern("\\d+");
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue("abc", "$.code"));
            assertTrue(error.getMessage().contains("$.code"));
            assertTrue(error.getMessage().contains("\\d+"));
        }

        @Test
        void shouldPassWhenPatternMatches() {
            BodyAssertion<String> assertion = Assertions.matchesPattern("\\d+");
            assertDoesNotThrow(() -> assertion.assertValue("12345", "$.code"));
        }
    }

    @Nested
    class AllMatchMessages {

        @Test
        void shouldFailWhenNotAllMatch() {
            BodyAssertion<Object> assertion = Assertions.allMatch(v -> ((Number) v).intValue() > 0);
            java.util.List<Integer> list = java.util.List.of(1, 2, -1, 4);
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(list, "$.values"));
            assertTrue(error.getMessage().contains("$.values"));
        }
    }

    @Nested
    class AnyMatchMessages {

        @Test
        void shouldFailWhenNoneMatch() {
            BodyAssertion<Object> assertion = Assertions.anyMatch(v -> ((Number) v).intValue() > 100);
            java.util.List<Integer> list = java.util.List.of(1, 2, 3);
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(list, "$.values"));
            assertTrue(error.getMessage().contains("$.values"));
            assertTrue(error.getMessage().contains("predicate"));
        }

        @Test
        void shouldPassWhenAtLeastOneMatches() {
            BodyAssertion<Object> assertion = Assertions.anyMatch(v -> ((Number) v).intValue() > 2);
            java.util.List<Integer> list = java.util.List.of(1, 2, 3);
            assertDoesNotThrow(() -> assertion.assertValue(list, "$.values"));
        }
    }

    @Nested
    class ContainsAllMessages {

        @Test
        void shouldFailWhenNotAllContained() {
            BodyAssertion<Object> assertion = Assertions.containsAll("a", "z");
            java.util.List<String> list = java.util.List.of("a", "b", "c");
            AssertionError error = assertThrows(AssertionError.class,
                    () -> assertion.assertValue(list, "$.items"));
            assertTrue(error.getMessage().contains("$.items"));
            assertTrue(error.getMessage().contains("z"));
        }

        @Test
        void shouldPassWhenAllContained() {
            BodyAssertion<Object> assertion = Assertions.containsAll("a", "c");
            java.util.List<String> list = java.util.List.of("a", "b", "c");
            assertDoesNotThrow(() -> assertion.assertValue(list, "$.items"));
        }
    }
}
