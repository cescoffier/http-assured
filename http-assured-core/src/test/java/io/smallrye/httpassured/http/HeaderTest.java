package io.smallrye.httpassured.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Header} — mirrors REST Assured's HeaderTest.
 */
class HeaderTest {

    @Nested
    class HasSameNameAs {

        @Test
        void shouldMatchSameNameCaseInsensitive() {
            Header h1 = new Header("foo", "bar");
            Header h2 = new Header("Foo", "baz");
            assertTrue(h1.hasSameNameAs(h2));
        }

        @Test
        void shouldNotMatchDifferentName() {
            Header h1 = new Header("foo", "bar");
            Header h2 = new Header("bar", "baz");
            assertFalse(h1.hasSameNameAs(h2));
        }

        @Test
        void shouldMatchExactSameName() {
            Header h1 = new Header("Content-Type", "application/json");
            Header h2 = new Header("Content-Type", "text/plain");
            assertTrue(h1.hasSameNameAs(h2));
        }

        @Test
        void shouldNotMatchNull() {
            Header h1 = new Header("foo", "bar");
            assertFalse(h1.hasSameNameAs(null));
        }
    }

    @Nested
    class Construction {

        @Test
        void shouldStoreNameAndValue() {
            Header header = new Header("X-Custom", "test-value");
            assertEquals("X-Custom", header.name());
            assertEquals("test-value", header.value());
        }

        @Test
        void shouldRejectNullName() {
            assertThrows(NullPointerException.class, () -> new Header(null, "value"));
        }

        @Test
        void shouldRejectNullValue() {
            assertThrows(NullPointerException.class, () -> new Header("name", null));
        }
    }

    @Nested
    class Equality {

        @Test
        void shouldBeEqualWhenSameNameAndValue() {
            Header h1 = new Header("foo", "bar");
            Header h2 = new Header("foo", "bar");
            assertEquals(h1, h2);
        }

        @Test
        void shouldNotBeEqualWhenDifferentName() {
            Header h1 = new Header("foo", "bar");
            Header h2 = new Header("baz", "bar");
            assertNotEquals(h1, h2);
        }

        @Test
        void shouldNotBeEqualWhenDifferentValue() {
            Header h1 = new Header("foo", "bar");
            Header h2 = new Header("foo", "baz");
            assertNotEquals(h1, h2);
        }

        @Test
        void shouldNotEqualNull() {
            Header h1 = new Header("foo", "bar");
            assertNotEquals(null, h1);
        }
    }
}
