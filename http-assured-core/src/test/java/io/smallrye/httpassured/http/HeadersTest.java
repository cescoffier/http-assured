package io.smallrye.httpassured.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadersTest {

    @Nested
    class Construction {

        @Test
        void shouldCreateEmptyHeaders() {
            Headers headers = new Headers();
            assertEquals(0, headers.size());
        }

        @Test
        void shouldCreateFromList() {
            Headers headers = new Headers(List.of(
                    new Header("Content-Type", "application/json"),
                    new Header("Accept", "text/html")
            ));
            assertEquals(2, headers.size());
        }

        @Test
        void shouldBeImmutable() {
            List<Header> list = new java.util.ArrayList<>();
            list.add(new Header("X-Test", "value"));
            Headers headers = new Headers(list);

            list.add(new Header("X-Other", "value"));
            assertEquals(1, headers.size());
        }
    }

    @Nested
    class GetValue {

        @Test
        void shouldReturnValueCaseInsensitive() {
            Headers headers = new Headers(List.of(
                    new Header("Content-Type", "application/json")
            ));
            assertEquals("application/json", headers.getValue("content-type").orElse(null));
            assertEquals("application/json", headers.getValue("CONTENT-TYPE").orElse(null));
        }

        @Test
        void shouldReturnEmptyForMissing() {
            Headers headers = new Headers();
            assertTrue(headers.getValue("Missing").isEmpty());
        }

        @Test
        void shouldReturnFirstValueForDuplicates() {
            Headers headers = new Headers(List.of(
                    new Header("X-Custom", "first"),
                    new Header("X-Custom", "second")
            ));
            assertEquals("first", headers.getValue("X-Custom").orElse(null));
        }
    }

    @Nested
    class GetValues {

        @Test
        void shouldReturnAllValuesForName() {
            Headers headers = new Headers(List.of(
                    new Header("Set-Cookie", "a=1"),
                    new Header("Set-Cookie", "b=2"),
                    new Header("Other", "value")
            ));
            List<String> values = headers.getValues("Set-Cookie");
            assertEquals(2, values.size());
            assertEquals("a=1", values.get(0));
            assertEquals("b=2", values.get(1));
        }

        @Test
        void shouldReturnEmptyListForMissing() {
            Headers headers = new Headers();
            assertTrue(headers.getValues("Missing").isEmpty());
        }
    }

    @Nested
    class HasHeader {

        @Test
        void shouldReturnTrueWhenPresent() {
            Headers headers = new Headers(List.of(new Header("X-Test", "val")));
            assertTrue(headers.hasHeader("x-test"));
        }

        @Test
        void shouldReturnFalseWhenAbsent() {
            Headers headers = new Headers();
            assertFalse(headers.hasHeader("Missing"));
        }
    }

    @Nested
    class With {

        @Test
        void shouldAddHeaderImmutably() {
            Headers original = new Headers();
            Headers updated = original.with("X-New", "value");

            assertEquals(0, original.size());
            assertEquals(1, updated.size());
            assertEquals("value", updated.getValue("X-New").orElse(null));
        }
    }

    @Nested
    class Merge {

        @Test
        void shouldMergeTwoHeaders() {
            Headers a = new Headers(List.of(new Header("A", "1")));
            Headers b = new Headers(List.of(new Header("B", "2")));
            Headers merged = a.merge(b);

            assertEquals(2, merged.size());
            assertTrue(merged.hasHeader("A"));
            assertTrue(merged.hasHeader("B"));
        }

        @Test
        void shouldPreserveOriginals() {
            Headers a = new Headers(List.of(new Header("A", "1")));
            Headers b = new Headers(List.of(new Header("B", "2")));
            a.merge(b);

            assertEquals(1, a.size());
            assertEquals(1, b.size());
        }
    }

    @Nested
    class Iteration {

        @Test
        void shouldIterateOverHeaders() {
            Headers headers = new Headers(List.of(
                    new Header("A", "1"),
                    new Header("B", "2")
            ));
            int count = 0;
            for (Header h : headers) {
                count++;
            }
            assertEquals(2, count);
        }
    }
}
