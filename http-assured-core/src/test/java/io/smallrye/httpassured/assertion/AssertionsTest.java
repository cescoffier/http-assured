package io.smallrye.httpassured.assertion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssertionsTest {

    @Nested
    class IsEqualTo {

        @Test
        void shouldPassWhenEqual() {
            BodyAssertion<String> assertion = Assertions.isEqualTo("hello");
            assertDoesNotThrow(() -> assertion.assertValue("hello", "$.name"));
        }

        @Test
        void shouldFailWhenNotEqual() {
            BodyAssertion<String> assertion = Assertions.isEqualTo("hello");
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue("world", "$.name"));
        }

        @Test
        void shouldWorkWithIntegers() {
            BodyAssertion<Integer> assertion = Assertions.isEqualTo(42);
            assertDoesNotThrow(() -> assertion.assertValue(42, "$.count"));
        }
    }

    @Nested
    class IsNotEqualTo {

        @Test
        void shouldPassWhenDifferent() {
            BodyAssertion<String> assertion = Assertions.isNotEqualTo("hello");
            assertDoesNotThrow(() -> assertion.assertValue("world", "$.name"));
        }

        @Test
        void shouldFailWhenEqual() {
            BodyAssertion<String> assertion = Assertions.isNotEqualTo("hello");
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue("hello", "$.name"));
        }
    }

    @Nested
    class IsNullTest {

        @Test
        void shouldPassWhenNull() {
            BodyAssertion<Object> assertion = Assertions.isNull();
            assertDoesNotThrow(() -> assertion.assertValue(null, "$.missing"));
        }

        @Test
        void shouldFailWhenNotNull() {
            BodyAssertion<Object> assertion = Assertions.isNull();
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue("value", "$.name"));
        }
    }

    @Nested
    class IsNotNullTest {

        @Test
        void shouldPassWhenNotNull() {
            BodyAssertion<Object> assertion = Assertions.isNotNull();
            assertDoesNotThrow(() -> assertion.assertValue("value", "$.name"));
        }

        @Test
        void shouldFailWhenNull() {
            BodyAssertion<Object> assertion = Assertions.isNotNull();
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(null, "$.name"));
        }
    }

    @Nested
    class Satisfies {

        @Test
        void shouldPassWhenPredicateMatches() {
            BodyAssertion<Integer> assertion = Assertions.satisfies(v -> v > 10);
            assertDoesNotThrow(() -> assertion.assertValue(42, "$.age"));
        }

        @Test
        void shouldFailWhenPredicateDoesNotMatch() {
            BodyAssertion<Integer> assertion = Assertions.satisfies(v -> v > 100);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(42, "$.age"));
        }
    }

    @Nested
    class HasSizeTest {

        @Test
        void shouldPassWhenSizeMatches() {
            BodyAssertion<List<String>> assertion = Assertions.hasSize(3);
            assertDoesNotThrow(() -> assertion.assertValue(List.of("a", "b", "c"), "$.items"));
        }

        @Test
        void shouldFailWhenSizeDoesNotMatch() {
            BodyAssertion<List<String>> assertion = Assertions.hasSize(5);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(List.of("a", "b"), "$.items"));
        }

        @Test
        void shouldFailWhenNull() {
            BodyAssertion<List<String>> assertion = Assertions.hasSize(3);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(null, "$.items"));
        }
    }

    @Nested
    class AllMatchTest {

        @Test
        void shouldPassWhenAllMatch() {
            BodyAssertion<List<Integer>> assertion = Assertions.allMatch(v -> (int) v > 0);
            assertDoesNotThrow(() -> assertion.assertValue(List.of(1, 2, 3), "$.prices"));
        }

        @Test
        void shouldFailWhenOneDoesNotMatch() {
            BodyAssertion<List<Integer>> assertion = Assertions.allMatch(v -> (int) v > 0);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(List.of(1, -2, 3), "$.prices"));
        }

        @Test
        void shouldFailWhenNull() {
            BodyAssertion<List<Integer>> assertion = Assertions.allMatch(v -> (int) v > 0);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(null, "$.prices"));
        }
    }

    @Nested
    class AnyMatchTest {

        @Test
        void shouldPassWhenAtLeastOneMatches() {
            BodyAssertion<List<Integer>> assertion = Assertions.anyMatch(v -> (int) v > 5);
            assertDoesNotThrow(() -> assertion.assertValue(List.of(1, 2, 10), "$.prices"));
        }

        @Test
        void shouldFailWhenNoneMatch() {
            BodyAssertion<List<Integer>> assertion = Assertions.anyMatch(v -> (int) v > 100);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(List.of(1, 2, 3), "$.prices"));
        }
    }

    @Nested
    class ContainsTest {

        @Test
        void shouldPassWhenContains() {
            BodyAssertion<List<String>> assertion = Assertions.contains("b");
            assertDoesNotThrow(() -> assertion.assertValue(List.of("a", "b", "c"), "$.items"));
        }

        @Test
        void shouldFailWhenDoesNotContain() {
            BodyAssertion<List<String>> assertion = Assertions.contains("z");
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(List.of("a", "b"), "$.items"));
        }
    }

    @Nested
    class ContainsAllTest {

        @Test
        void shouldPassWhenContainsAll() {
            BodyAssertion<List<String>> assertion = Assertions.containsAll("a", "c");
            assertDoesNotThrow(() -> assertion.assertValue(List.of("a", "b", "c"), "$.items"));
        }

        @Test
        void shouldFailWhenMissing() {
            BodyAssertion<List<String>> assertion = Assertions.containsAll("a", "z");
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(List.of("a", "b"), "$.items"));
        }
    }

    @Nested
    class ContainsStringTest {

        @Test
        void shouldPassWhenContains() {
            BodyAssertion<String> assertion = Assertions.containsString("ell");
            assertDoesNotThrow(() -> assertion.assertValue("hello", "$.greeting"));
        }

        @Test
        void shouldFailWhenDoesNotContain() {
            BodyAssertion<String> assertion = Assertions.containsString("xyz");
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue("hello", "$.greeting"));
        }
    }

    @Nested
    class MatchesPatternTest {

        @Test
        void shouldPassWhenMatches() {
            BodyAssertion<String> assertion = Assertions.matchesPattern("\\d{3}-\\d{4}");
            assertDoesNotThrow(() -> assertion.assertValue("123-4567", "$.phone"));
        }

        @Test
        void shouldFailWhenDoesNotMatch() {
            BodyAssertion<String> assertion = Assertions.matchesPattern("\\d{3}-\\d{4}");
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue("abc", "$.phone"));
        }
    }

    @Nested
    class GreaterThanTest {

        @Test
        void shouldPassWhenGreater() {
            BodyAssertion<Integer> assertion = Assertions.greaterThan(10);
            assertDoesNotThrow(() -> assertion.assertValue(42, "$.count"));
        }

        @Test
        void shouldFailWhenEqual() {
            BodyAssertion<Integer> assertion = Assertions.greaterThan(42);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(42, "$.count"));
        }

        @Test
        void shouldFailWhenLess() {
            BodyAssertion<Integer> assertion = Assertions.greaterThan(100);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(42, "$.count"));
        }
    }

    @Nested
    class LessThanTest {

        @Test
        void shouldPassWhenLess() {
            BodyAssertion<Integer> assertion = Assertions.lessThan(100);
            assertDoesNotThrow(() -> assertion.assertValue(42, "$.count"));
        }

        @Test
        void shouldFailWhenGreater() {
            BodyAssertion<Integer> assertion = Assertions.lessThan(10);
            assertThrows(AssertionFailedError.class,
                    () -> assertion.assertValue(42, "$.count"));
        }
    }
}
