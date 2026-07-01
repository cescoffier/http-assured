package io.smallrye.httpassured.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Cookie}.
 */
class CookieTest {

    @Nested
    class Construction {

        @Test
        void shouldStoreNameAndValue() {
            Cookie c = new Cookie.Builder("session_id", "1234").build();
            assertEquals("session_id", c.name());
            assertEquals("1234", c.value());
        }

        @Test
        void shouldHaveDefaultsWhenNoAttributesSet() {
            Cookie c = new Cookie.Builder("a", "b").build();
            assertFalse(c.isSecured());
            assertNull(c.comment());
        }

        @Test
        void shouldRejectNullName() {
            assertThrows(NullPointerException.class, () -> new Cookie.Builder(null, "value"));
        }

        @Test
        void shouldRejectNullValue() {
            assertThrows(NullPointerException.class, () -> new Cookie.Builder("name", null));
        }
    }

    @Nested
    class Attributes {

        @Test
        void shouldSetSecured() {
            Cookie c = new Cookie.Builder("session_id", "1234")
                    .secure(true)
                    .build();
            assertTrue(c.isSecured());
        }

        @Test
        void shouldSetComment() {
            Cookie c = new Cookie.Builder("session_id", "1234")
                    .comment("session id cookie")
                    .build();
            assertEquals("session id cookie", c.comment());
        }

        @Test
        void shouldSetAllAttributes() {
            Cookie c = new Cookie.Builder("session_id", "1234")
                    .secure(true)
                    .comment("session id cookie")
                    .build();
            assertEquals("session_id", c.name());
            assertEquals("1234", c.value());
            assertTrue(c.isSecured());
            assertEquals("session id cookie", c.comment());
        }
    }

    @Nested
    class ToHeaderValue {

        @Test
        void shouldReturnNameEqualsValue() {
            Cookie c = new Cookie.Builder("session_id", "1234").build();
            assertEquals("session_id=1234", c.toHeaderValue());
        }

        @Test
        void shouldNotIncludeAttributes() {
            Cookie c = new Cookie.Builder("token", "abc")
                    .secure(true)
                    .comment("my cookie")
                    .build();
            assertEquals("token=abc", c.toHeaderValue());
        }

        @Test
        void shouldHandleValueWithSpecialCharacters() {
            Cookie c = new Cookie.Builder("data", "hello world").build();
            assertEquals("data=hello world", c.toHeaderValue());
        }
    }

    @Nested
    class BuilderChaining {

        @Test
        void shouldReturnBuilderFromSetSecured() {
            Cookie.Builder builder = new Cookie.Builder("a", "b");
            assertEquals(builder, builder.secure(true));
        }

        @Test
        void shouldReturnBuilderFromSetComment() {
            Cookie.Builder builder = new Cookie.Builder("a", "b");
            assertEquals(builder, builder.comment("x"));
        }
    }
}
