package io.smallrye.httpassured.path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPathEvaluatorTest {

    private static final String SIMPLE_JSON = """
            {
                "name": "John",
                "age": 30,
                "active": true
            }
            """;

    private static final String NESTED_JSON = """
            {
                "store": {
                    "book": [
                        { "title": "Sayings of the Century", "price": 8.95 },
                        { "title": "Sword of Honour", "price": 12.99 },
                        { "title": "The Lord of the Rings", "price": 22.99 }
                    ],
                    "bicycle": {
                        "color": "red",
                        "price": 19.95
                    }
                }
            }
            """;

    private static final String ARRAY_JSON = """
            {
                "users": [
                    { "name": "Alice", "age": 25 },
                    { "name": "Bob", "age": 35 },
                    { "name": "Charlie", "age": 17 }
                ]
            }
            """;

    // REST Assured-style store JSON (mirrors their JSON constant)
    private static final String STORE_JSON = """
            {
                "store": {
                    "book": [
                        {
                            "category": "reference",
                            "author": "Nigel Rees",
                            "title": "Sayings of the Century",
                            "price": 8.95
                        },
                        {
                            "category": "fiction",
                            "author": "Evelyn Waugh",
                            "title": "Sword of Honour",
                            "price": 12.99
                        },
                        {
                            "category": "fiction",
                            "author": "Herman Melville",
                            "title": "Moby Dick",
                            "price": 8.99
                        },
                        {
                            "category": "fiction",
                            "author": "J. R. R. Tolkien",
                            "title": "The Lord of the Rings",
                            "price": 22.99
                        }
                    ],
                    "bicycle": {
                        "color": "red",
                        "price": 19.95
                    }
                }
            }
            """;

    // Root-level JSON array (unnamed root)
    private static final String ROOT_ARRAY_JSON = """
            [
                { "email": "a@b.com", "alias": "ab", "phone": "1234" },
                { "email": "c@d.com", "alias": "cd", "phone": "5678" },
                { "email": "e@f.com", "alias": "ef", "phone": "9012" }
            ]
            """;

    // JSON with numeric keys
    private static final String NUMERIC_KEY_JSON = """
            {
                "0": { "name": "first" },
                "1": { "name": "second" }
            }
            """;

    // JSON with special attribute names
    private static final String SPECIAL_KEYS_JSON = """
            {
                "properties": { "prop1": "value1" },
                "size": 42,
                "class": "MyClass",
                "some-key": "hyphen-value",
                "true": "boolean-key",
                "items": [
                    { "name": "a" },
                    { "name": "b" }
                ]
            }
            """;

    // JSON with map-like structure
    private static final String MAP_JSON = """
            {
                "price1": 12.3,
                "price2": 15.0
            }
            """;

    @Nested
    class NormalizePath {

        @Test
        void shouldReturnDollarForNull() {
            assertEquals("$", JsonPathEvaluator.normalizePath(null));
        }

        @Test
        void shouldReturnDollarForEmpty() {
            assertEquals("$", JsonPathEvaluator.normalizePath(""));
        }

        @Test
        void shouldPreserveDollarPrefix() {
            assertEquals("$.name", JsonPathEvaluator.normalizePath("$.name"));
        }

        @Test
        void shouldPreserveDollarBracket() {
            assertEquals("$['name']", JsonPathEvaluator.normalizePath("$['name']"));
        }

        @Test
        void shouldPrependDollarDotForBarePath() {
            assertEquals("$.name", JsonPathEvaluator.normalizePath("name"));
        }

        @Test
        void shouldPrependDollarDotForNestedBarePath() {
            assertEquals("$.store.book[0].title", JsonPathEvaluator.normalizePath("store.book[0].title"));
        }
    }

    @Nested
    class EvaluateSimple {

        @Test
        void shouldExtractStringWithBarePath() {
            String result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "name");
            assertEquals("John", result);
        }

        @Test
        void shouldExtractStringWithDollarPath() {
            String result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "$.name");
            assertEquals("John", result);
        }

        @Test
        void shouldExtractInteger() {
            Integer result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "age");
            assertEquals(30, result);
        }

        @Test
        void shouldExtractBoolean() {
            Boolean result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "active");
            assertTrue(result);
        }

        @Test
        void shouldReturnNullForMissingPath() {
            Object result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "nonexistent");
            assertNull(result);
        }
    }

    @Nested
    class EvaluateNested {

        @Test
        void shouldExtractNestedValue() {
            String result = JsonPathEvaluator.evaluate(NESTED_JSON, "store.bicycle.color");
            assertEquals("red", result);
        }

        @Test
        void shouldExtractFromArray() {
            String result = JsonPathEvaluator.evaluate(NESTED_JSON, "store.book[0].title");
            assertEquals("Sayings of the Century", result);
        }

        @Test
        void shouldExtractLastArrayElement() {
            String result = JsonPathEvaluator.evaluate(NESTED_JSON, "store.book[2].title");
            assertEquals("The Lord of the Rings", result);
        }

        @Test
        void shouldExtractArraySize() {
            List<?> result = JsonPathEvaluator.evaluate(NESTED_JSON, "store.book");
            assertEquals(3, result.size());
        }
    }

    @Nested
    class EvaluateAdvancedJsonPath {

        @Test
        void shouldExtractAllTitles() {
            List<String> result = JsonPathEvaluator.evaluate(NESTED_JSON, "$.store.book[*].title");
            assertEquals(3, result.size());
            assertTrue(result.contains("Sayings of the Century"));
            assertTrue(result.contains("Sword of Honour"));
            assertTrue(result.contains("The Lord of the Rings"));
        }

        @Test
        void shouldFilterByPredicate() {
            List<?> result = JsonPathEvaluator.evaluate(NESTED_JSON, "$.store.book[?(@.price < 10)]");
            assertEquals(1, result.size());
        }

        @Test
        void shouldExtractAllPrices() {
            List<Double> result = JsonPathEvaluator.evaluate(NESTED_JSON, "$.store.book[*].price");
            assertEquals(3, result.size());
            assertTrue(result.stream().allMatch(p -> p > 0));
        }
    }

    @Nested
    class EvaluateWithType {

        @Test
        void shouldEvaluateAsSpecificType() {
            String result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "name", String.class);
            assertEquals("John", result);
        }

        @Test
        void shouldEvaluateAsInteger() {
            Integer result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "age", Integer.class);
            assertEquals(30, result);
        }
    }


    @Nested
    class RootObjectExtraction {

        @Test
        void shouldGetEntireObjectGraphUsingDollar() {
            Map<String, Object> result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "$");
            assertNotNull(result);
            assertEquals("John", result.get("name"));
            assertEquals(30, result.get("age"));
            assertEquals(true, result.get("active"));
        }

        @Test
        void shouldGetEntireObjectGraphUsingEmptyPath() {
            // Empty path normalizes to "$" which returns root
            Map<String, Object> result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "");
            assertNotNull(result);
            assertEquals("John", result.get("name"));
        }

        @Test
        void shouldGetEntireObjectGraphUsingNullPath() {
            Map<String, Object> result = JsonPathEvaluator.evaluate(SIMPLE_JSON, null);
            assertNotNull(result);
            assertEquals("John", result.get("name"));
        }

        @Test
        void shouldGetRootObjectAsMap() {
            Map<String, Object> result = JsonPathEvaluator.evaluate(MAP_JSON, "$");
            assertNotNull(result);
            assertEquals(12.3, result.get("price1"));
            assertEquals(15.0, result.get("price2"));
        }

        @Test
        void shouldGetNestedObjectAsMap() {
            Map<String, Object> result = JsonPathEvaluator.evaluate(STORE_JSON, "store.bicycle");
            assertNotNull(result);
            assertEquals("red", result.get("color"));
            assertEquals(19.95, result.get("price"));
        }
    }


    @Nested
    class UnnamedRootArray {

        @Test
        void shouldGetValueFromUnnamedRootArrayByIndex() {
            String email = JsonPathEvaluator.evaluate(ROOT_ARRAY_JSON, "$[0].email");
            assertEquals("a@b.com", email);
        }

        @Test
        void shouldGetLastValueFromUnnamedRootArray() {
            String email = JsonPathEvaluator.evaluate(ROOT_ARRAY_JSON, "$[2].email");
            assertEquals("e@f.com", email);
        }

        @Test
        void shouldGetSubValueFromUnnamedRootArray() {
            String alias = JsonPathEvaluator.evaluate(ROOT_ARRAY_JSON, "$[1].alias");
            assertEquals("cd", alias);
        }

        @Test
        void shouldGetAllValuesFromUnnamedRootArray() {
            List<String> emails = JsonPathEvaluator.evaluate(ROOT_ARRAY_JSON, "$[*].email");
            assertEquals(3, emails.size());
            assertEquals("a@b.com", emails.get(0));
            assertEquals("c@d.com", emails.get(1));
            assertEquals("e@f.com", emails.get(2));
        }

        @Test
        void shouldGetEntireRootArray() {
            List<?> result = JsonPathEvaluator.evaluate(ROOT_ARRAY_JSON, "$");
            assertNotNull(result);
            assertEquals(3, result.size());
        }
    }


    @Nested
    class TypeConversion {

        @Test
        void shouldConvertValueToStringWhenExplicitlyRequested() {
            String result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "age", String.class);
            assertEquals("30", result);
        }

        @Test
        void shouldConvertValueToIntegerWhenExplicitlyRequested() {
            Integer result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "age", Integer.class);
            assertEquals(30, result);
        }

        @Test
        void shouldConvertValueToDoubleWhenExplicitlyRequested() {
            Double result = JsonPathEvaluator.evaluate(STORE_JSON, "store.book[0].price", Double.class);
            assertEquals(8.95, result, 0.001);
        }

        @Test
        void shouldConvertIntToStringWhenRequested() {
            // The store has price 8.95, we can read it as string
            String result = JsonPathEvaluator.evaluate(STORE_JSON, "store.book[0].price", String.class);
            assertNotNull(result);
            assertTrue(result.contains("8.95"));
        }

        @Test
        void shouldGetStringDirectly() {
            // getString equivalent — evaluate with String.class
            String result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "name", String.class);
            assertEquals("John", result);
        }
    }


    @Nested
    class GenericListAndMap {

        @Test
        void shouldGetListOfCategories() {
            List<String> categories = JsonPathEvaluator.evaluate(STORE_JSON, "store.book[*].category");
            assertEquals(4, categories.size());
            assertEquals("reference", categories.get(0));
            assertEquals("fiction", categories.get(1));
        }

        @Test
        void shouldGetListOfBooks() {
            List<Map<String, Object>> books = JsonPathEvaluator.evaluate(STORE_JSON, "store.book");
            assertEquals(4, books.size());
            assertEquals("Nigel Rees", books.get(0).get("author"));
            assertEquals("Evelyn Waugh", books.get(1).get("author"));
        }

        @Test
        void shouldConvertListMembersToType() {
            List<Double> prices = JsonPathEvaluator.evaluate(STORE_JSON, "store.book[*].price");
            assertEquals(4, prices.size());
            assertTrue(prices.stream().allMatch(p -> p instanceof Number));
        }

        @Test
        void shouldGetMapFromNestedObject() {
            Map<String, Object> bicycle = JsonPathEvaluator.evaluate(STORE_JSON, "store.bicycle");
            assertEquals("red", bicycle.get("color"));
            assertEquals(19.95, bicycle.get("price"));
        }
    }


    @Nested
    class FilterAndSize {

        @Test
        void shouldFilterBooksBetweenPriceRange() {
            List<?> books = JsonPathEvaluator.evaluate(STORE_JSON,
                    "$.store.book[?(@.price >= 5 && @.price <= 15)]");
            assertEquals(3, books.size()); // 8.95, 12.99, 8.99
        }

        @Test
        void shouldFilterBooksByAuthor() {
            List<?> books = JsonPathEvaluator.evaluate(STORE_JSON,
                    "$.store.book[?(@.author == 'Nigel Rees')]");
            assertEquals(1, books.size());
        }

        @Test
        void shouldGetSizeViaLength() {
            // Jayway supports .length() for arrays
            Integer size = JsonPathEvaluator.evaluate(STORE_JSON, "$.store.book.length()");
            assertEquals(4, size);
        }

        @Test
        void shouldGetFirstBookCategory() {
            String category = JsonPathEvaluator.evaluate(STORE_JSON, "store.book[0].category");
            assertEquals("reference", category);
        }

        @Test
        void shouldGetLastBookTitle() {
            String title = JsonPathEvaluator.evaluate(STORE_JSON, "store.book[3].title");
            assertEquals("The Lord of the Rings", title);
        }
    }


    @Nested
    class SpecialAttributeNames {

        @Test
        void shouldParseAttributeNamedProperties() {
            Map<String, Object> result = JsonPathEvaluator.evaluate(SPECIAL_KEYS_JSON, "properties");
            assertNotNull(result);
            assertEquals("value1", result.get("prop1"));
        }

        @Test
        void shouldParseAttributeNamedSize() {
            Integer result = JsonPathEvaluator.evaluate(SPECIAL_KEYS_JSON, "size");
            assertEquals(42, result);
        }

        @Test
        void shouldParseAttributeNamedClass() {
            // "class" is a reserved word in Java but not in JSON
            String result = JsonPathEvaluator.evaluate(SPECIAL_KEYS_JSON, "$.class");
            assertEquals("MyClass", result);
        }

        @Test
        void shouldParseAttributeWithHyphen() {
            String result = JsonPathEvaluator.evaluate(SPECIAL_KEYS_JSON, "$['some-key']");
            assertEquals("hyphen-value", result);
        }

        @Test
        void shouldParseAttributeWithHyphenWithoutBrackets() {
            // Jayway supports dot notation for hyphenated keys
            String result = JsonPathEvaluator.evaluate(SPECIAL_KEYS_JSON, "$.some-key");
            assertEquals("hyphen-value", result);
        }

        @Test
        void shouldParseBooleanKeyName() {
            String result = JsonPathEvaluator.evaluate(SPECIAL_KEYS_JSON, "$['true']");
            assertEquals("boolean-key", result);
        }

        @Test
        void shouldParseNumericKeyWithBrackets() {
            String name = JsonPathEvaluator.evaluate(NUMERIC_KEY_JSON, "$['0'].name");
            assertEquals("first", name);
        }

        @Test
        void shouldParseSecondNumericKey() {
            String name = JsonPathEvaluator.evaluate(NUMERIC_KEY_JSON, "$['1'].name");
            assertEquals("second", name);
        }
    }


    @Nested
    class MalformedJson {

        @Test
        void shouldReturnNullForMalformedJson() {
            // With SUPPRESS_EXCEPTIONS, malformed JSON returns null
            Object result = JsonPathEvaluator.evaluate("{ invalid json }", "name");
            assertNull(result);
        }

        @Test
        void shouldReturnNullForCompletelyInvalidJson() {
            Object result = JsonPathEvaluator.evaluate("not json at all", "anything");
            assertNull(result);
        }

        @Test
        void shouldReturnNullForEmptyString() {
            Object result = JsonPathEvaluator.evaluate("", "name");
            assertNull(result);
        }
    }


    @Nested
    class AbsentKeysAndLists {

        @Test
        void shouldReturnNullForAbsentJsonKeys() {
            Object result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "nonexistent");
            assertNull(result);
        }

        @Test
        void shouldReturnNullForAbsentNestedKey() {
            Object result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "deeply.nested.missing");
            assertNull(result);
        }

        @Test
        void shouldNotFailOnAbsentLists() {
            // Accessing a list index on a non-existent array should return null
            Object result = JsonPathEvaluator.evaluate(SIMPLE_JSON, "$.items[0].name");
            assertNull(result);
        }

        @Test
        void shouldReturnNullForArrayIndexOutOfBounds() {
            Object result = JsonPathEvaluator.evaluate(STORE_JSON, "store.book[99].title");
            assertNull(result);
        }

        @Test
        void shouldHandleNullValueInJson() {
            String json = """
                    { "name": "test", "value": null }
                    """;
            Object result = JsonPathEvaluator.evaluate(json, "value");
            assertNull(result);
        }
    }


    @Nested
    class PrimitiveRootValues {

        @Test
        void shouldNotFailOnPrimitiveString() {
            String json = "\"hello\"";
            Object result = JsonPathEvaluator.evaluate(json, "$");
            assertEquals("hello", result);
        }

        @Test
        void shouldNotFailOnPrimitiveTrue() {
            String json = "true";
            Object result = JsonPathEvaluator.evaluate(json, "$");
            assertEquals(true, result);
        }

        @Test
        void shouldNotFailOnPrimitiveFalse() {
            String json = "false";
            Object result = JsonPathEvaluator.evaluate(json, "$");
            assertEquals(false, result);
        }

        @Test
        void shouldNotFailOnPrimitiveNull() {
            // Jayway's JSON parser doesn't accept bare "null" as valid JSON,
            // so our evaluator gracefully returns null
            String json = "null";
            Object result = JsonPathEvaluator.evaluate(json, "$");
            assertNull(result);
        }

        @Test
        void shouldNotFailOnPrimitiveNumber() {
            String json = "42";
            Object result = JsonPathEvaluator.evaluate(json, "$");
            assertEquals(42, result);
        }

        @Test
        void shouldNotFailOnPrimitiveDecimal() {
            String json = "3.14";
            Object result = JsonPathEvaluator.evaluate(json, "$");
            assertEquals(3.14, result);
        }
    }


    @Nested
    class ComplexStoreQueries {

        @Test
        void shouldGetAllAuthors() {
            List<String> authors = JsonPathEvaluator.evaluate(STORE_JSON, "$.store.book[*].author");
            assertEquals(4, authors.size());
            assertTrue(authors.contains("Nigel Rees"));
            assertTrue(authors.contains("J. R. R. Tolkien"));
        }

        @Test
        void shouldFilterByCategory() {
            List<?> fiction = JsonPathEvaluator.evaluate(STORE_JSON,
                    "$.store.book[?(@.category == 'fiction')]");
            assertEquals(3, fiction.size());
        }

        @Test
        void shouldFilterBooksUnder10() {
            List<?> cheapBooks = JsonPathEvaluator.evaluate(STORE_JSON,
                    "$.store.book[?(@.price < 10)]");
            assertEquals(2, cheapBooks.size()); // 8.95 and 8.99
        }

        @Test
        void shouldGetBicycleColor() {
            String color = JsonPathEvaluator.evaluate(STORE_JSON, "store.bicycle.color");
            assertEquals("red", color);
        }

        @Test
        void shouldGetBicyclePrice() {
            Double price = JsonPathEvaluator.evaluate(STORE_JSON, "store.bicycle.price");
            assertEquals(19.95, price, 0.001);
        }

        @Test
        void shouldCheckKeyExistsViaEvaluation() {
            // If key exists, we get a non-null value
            Object name = JsonPathEvaluator.evaluate(SIMPLE_JSON, "name");
            assertNotNull(name);

            // If key doesn't exist, we get null
            Object missing = JsonPathEvaluator.evaluate(SIMPLE_JSON, "missing");
            assertNull(missing);
        }
    }


    @Nested
    class MultipleValues {

        @Test
        void shouldExtractMultipleValuesFromArray() {
            String json = """
                    {
                        "items": [
                            { "id": 1, "name": "a", "active": true },
                            { "id": 2, "name": "b", "active": false },
                            { "id": 3, "name": "c", "active": true }
                        ]
                    }
                    """;

            List<Integer> ids = JsonPathEvaluator.evaluate(json, "$.items[*].id");
            assertEquals(List.of(1, 2, 3), ids);

            List<String> names = JsonPathEvaluator.evaluate(json, "$.items[*].name");
            assertEquals(List.of("a", "b", "c"), names);

            List<Boolean> actives = JsonPathEvaluator.evaluate(json, "$.items[*].active");
            assertEquals(List.of(true, false, true), actives);
        }

        @Test
        void shouldFilterAndExtractMultipleValues() {
            String json = """
                    {
                        "items": [
                            { "id": 1, "name": "a", "active": true },
                            { "id": 2, "name": "b", "active": false },
                            { "id": 3, "name": "c", "active": true }
                        ]
                    }
                    """;

            List<?> activeItems = JsonPathEvaluator.evaluate(json,
                    "$.items[?(@.active == true)]");
            assertEquals(2, activeItems.size());
        }
    }


    @Nested
    class UnicodeHandling {

        @Test
        void shouldHandleUnicodeValues() {
            String json = """
                    { "name": "Ränder", "city": "Zürich", "emoji": "\\u2764" }
                    """;
            String name = JsonPathEvaluator.evaluate(json, "name");
            assertEquals("Ränder", name);

            String city = JsonPathEvaluator.evaluate(json, "city");
            assertEquals("Zürich", city);
        }

        @Test
        void shouldHandleUnicodeKeys() {
            String json = """
                    { "données": "value", "名前": "太郎" }
                    """;
            String result = JsonPathEvaluator.evaluate(json, "$.données");
            assertEquals("value", result);

            String name = JsonPathEvaluator.evaluate(json, "$.名前");
            assertEquals("太郎", name);
        }
    }


    @Nested
    class NumberHandling {

        @Test
        void shouldReturnIntegerForSmallWholeNumbers() {
            String json = """
                    { "count": 42 }
                    """;
            Object result = JsonPathEvaluator.evaluate(json, "count");
            assertInstanceOf(Integer.class, result);
            assertEquals(42, result);
        }

        @Test
        void shouldReturnDoubleForDecimalNumbers() {
            String json = """
                    { "price": 19.95 }
                    """;
            Object result = JsonPathEvaluator.evaluate(json, "price");
            assertInstanceOf(Double.class, result);
            assertEquals(19.95, (Double) result, 0.001);
        }

        @Test
        void shouldHandleLargeIntegerNumbers() {
            String json = """
                    { "big": 2147483648 }
                    """;
            Object result = JsonPathEvaluator.evaluate(json, "big");
            // Beyond int range, Jayway returns Long
            assertInstanceOf(Long.class, result);
            assertEquals(2147483648L, result);
        }

        @Test
        void shouldHandleNegativeNumbers() {
            String json = """
                    { "temp": -15, "balance": -99.5 }
                    """;
            Integer temp = JsonPathEvaluator.evaluate(json, "temp");
            assertEquals(-15, temp);

            Double balance = JsonPathEvaluator.evaluate(json, "balance");
            assertEquals(-99.5, balance, 0.001);
        }

        @Test
        void shouldHandleZero() {
            String json = """
                    { "zero": 0, "zeroFloat": 0.0 }
                    """;
            Integer zero = JsonPathEvaluator.evaluate(json, "zero");
            assertEquals(0, zero);

            Double zeroFloat = JsonPathEvaluator.evaluate(json, "zeroFloat");
            assertEquals(0.0, zeroFloat, 0.001);
        }
    }
}
