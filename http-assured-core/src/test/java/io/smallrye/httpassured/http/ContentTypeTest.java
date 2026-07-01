package io.smallrye.httpassured.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ContentType} — mirrors REST Assured's ContentTypeTest.
 */
class ContentTypeTest {

    @Nested
    class WithCharset {

        @Test
        void shouldAppendCharsetString() {
            String result = ContentType.JSON.withCharset("UTF-8");
            assertEquals("application/json; charset=UTF-8", result);
        }

        @Test
        void shouldAppendJavaCharset() {
            String result = ContentType.JSON.withCharset(StandardCharsets.ISO_8859_1);
            assertEquals("application/json; charset=ISO-8859-1", result);
        }

        @Test
        void shouldReturnValueWhenCharsetIsNull() {
            String result = ContentType.JSON.withCharset((String) null);
            assertEquals("application/json", result);
        }

        @Test
        void shouldReturnValueWhenCharsetIsEmpty() {
            String result = ContentType.JSON.withCharset("");
            assertEquals("application/json", result);
        }

        @Test
        void shouldReturnValueWhenJavaCharsetIsNull() {
            String result = ContentType.JSON.withCharset((java.nio.charset.Charset) null);
            assertEquals("application/json", result);
        }
    }

    @Nested
    class Matches {

        @Test
        void shouldMatchExactContentType() {
            assertTrue(ContentType.JSON.matches("application/json"));
        }

        @Test
        void shouldMatchCaseInsensitive() {
            assertTrue(ContentType.JSON.matches("appliCatIon/JSON"));
        }

        @Test
        void shouldMatchWithCharsetParameter() {
            assertTrue(ContentType.JSON.matches("application/json; charset=UTF-8"));
        }

        @Test
        void shouldNotMatchDifferentContentType() {
            assertFalse(ContentType.JSON.matches("application/json2"));
        }

        @Test
        void shouldNotMatchNull() {
            assertFalse(ContentType.JSON.matches(null));
        }

        @Test
        void shouldMatchTextPlain() {
            assertTrue(ContentType.TEXT.matches("text/plain"));
        }

        @Test
        void shouldMatchHtml() {
            assertTrue(ContentType.HTML.matches("text/html; charset=UTF-8"));
        }
    }

    @Nested
    class FromString {

        @Test
        void shouldFindJsonFromString() {
            assertEquals(ContentType.JSON, ContentType.fromString("application/json"));
        }

        @Test
        void shouldFindXmlFromString() {
            assertEquals(ContentType.XML, ContentType.fromString("application/xml"));
        }

        @Test
        void shouldFindTextFromString() {
            assertEquals(ContentType.TEXT, ContentType.fromString("text/plain"));
        }

        @Test
        void shouldFindHtmlFromString() {
            assertEquals(ContentType.HTML, ContentType.fromString("text/html"));
        }

        @Test
        void shouldFindFormUrlEncodedFromString() {
            assertEquals(ContentType.FORM_URL_ENCODED, ContentType.fromString("application/x-www-form-urlencoded"));
        }

        @Test
        void shouldFindMultipartFormDataFromString() {
            assertEquals(ContentType.MULTIPART_FORM_DATA, ContentType.fromString("multipart/form-data"));
        }

        @Test
        void shouldFindBinaryFromString() {
            assertEquals(ContentType.BINARY, ContentType.fromString("application/octet-stream"));
        }

        @Test
        void shouldFindContentTypeFromStringWithCharset() {
            ContentType result = ContentType.fromString("application/json; charset=UTF-8");
            assertEquals(ContentType.JSON, result);
        }

        @Test
        void shouldReturnNullForUnknownContentType() {
            assertNull(ContentType.fromString("application/unknown"));
        }

        @Test
        void shouldReturnNullForNull() {
            assertNull(ContentType.fromString(null));
        }

        @Test
        void shouldReturnNullForEmpty() {
            assertNull(ContentType.fromString(""));
        }

        @Test
        void shouldMatchCaseInsensitively() {
            ContentType result = ContentType.fromString("APPLICATION/JSON");
            assertEquals(ContentType.JSON, result);
        }
    }

    @Nested
    class WithoutCharset {

        @Test
        void shouldReturnContentTypeAsIsWhenNoCharset() {
            String result = ContentType.withoutCharset("application/json");
            assertEquals("application/json", result);
        }

        @Test
        void shouldStripCharset() {
            String result = ContentType.withoutCharset("application/json; charset=UTF-8");
            assertEquals("application/json", result);
        }

        @Test
        void shouldStripMultipleParameters() {
            String result = ContentType.withoutCharset("text/html; charset=UTF-8; boundary=something");
            assertEquals("text/html", result);
        }

        @Test
        void shouldHandleNull() {
            assertNull(ContentType.withoutCharset(null));
        }

        @Test
        void shouldHandleEmpty() {
            assertEquals("", ContentType.withoutCharset(""));
        }
    }

    @Nested
    class ValueAndToString {

        @Test
        void shouldReturnValue() {
            assertEquals("application/json", ContentType.JSON.value());
        }

        @Test
        void shouldReturnValueFromToString() {
            assertEquals("application/json", ContentType.JSON.toString());
        }

        @Test
        void shouldReturnCorrectXmlValue() {
            assertEquals("application/xml", ContentType.XML.value());
        }

        @Test
        void shouldReturnCorrectFormUrlEncodedValue() {
            assertEquals("application/x-www-form-urlencoded", ContentType.FORM_URL_ENCODED.value());
        }
    }
}
