package io.smallrye.httpassured.path;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.Jackson3JsonProvider;
import com.jayway.jsonpath.spi.mapper.Jackson3MappingProvider;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Evaluates JsonPath expressions against JSON strings.
 * <p>
 * Supports auto-detection of path syntax:
 * <ul>
 *   <li>Paths starting with {@code $} are used as-is (standard JSONPath)</li>
 *   <li>Bare paths like {@code users[0].name} are automatically prefixed with {@code $.}</li>
 * </ul>
 * </p>
 */
public final class JsonPathEvaluator {

    private static final Configuration DEFAULT_CONFIG = Configuration.builder()
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    private JsonPathEvaluator() {}

    /**
     * Evaluates a JSON path expression against a JSON string using the default configuration.
     *
     * @param json the JSON string to query
     * @param path the path expression (auto-detected syntax)
     * @param <T>  the expected result type
     * @return the value at the given path, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T evaluate(String json, String path) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        String normalizedPath = normalizePath(path);
        try {
            return JsonPath.using(DEFAULT_CONFIG).parse(json).read(normalizedPath);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Evaluates a JSON path expression against a JSON string, using the supplied
     * {@link ObjectMapper} to control how JSON values are parsed (e.g. floats as
     * {@code BigDecimal} when {@code USE_BIG_DECIMAL_FOR_FLOATS} is enabled).
     *
     * @param json         the JSON string to query
     * @param path         the path expression (auto-detected syntax)
     * @param objectMapper the mapper to use for parsing; if {@code null} falls back to the default
     * @param <T>          the expected result type
     * @return the value at the given path, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T evaluateWith(String json, String path, ObjectMapper objectMapper) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        String normalizedPath = normalizePath(path);
        Configuration config = objectMapper != null ? configFor(objectMapper) : DEFAULT_CONFIG;
        try {
            return JsonPath.using(config).parse(json).read(normalizedPath);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Evaluates a JSON path expression and returns the result as a specific type.
     *
     * @param json the JSON string to query
     * @param path the path expression
     * @param type the expected result class
     * @param <T>  the expected result type
     * @return the value at the given path
     */
    public static <T> T evaluate(String json, String path, Class<T> type) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        String normalizedPath = normalizePath(path);
        try {
            return JsonPath.using(DEFAULT_CONFIG).parse(json).read(normalizedPath, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Normalizes a path expression to standard JSONPath format.
     * <p>
     * If the path starts with {@code $}, it's returned as-is.
     * Otherwise, {@code $.} is prepended.
     * </p>
     *
     * @param path the input path
     * @return the normalized JSONPath expression
     */
    static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "$";
        }
        if (path.startsWith("$")) {
            return path;
        }
        return "$." + path;
    }

    private static Configuration configFor(ObjectMapper objectMapper) {
        // Build a lenient copy of the mapper that inherits the user's type-mapping settings
        // (e.g. USE_BIG_DECIMAL_FOR_FLOATS) but also tolerates non-standard JSON like
        // unquoted property names, which the strict Jackson 3 parser rejects by default.
        JsonMapper.Builder builder = JsonMapper.builder();
        if (objectMapper instanceof JsonMapper jm) {
            builder = jm.rebuild();
        }
        ObjectMapper lenient = builder
                .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
                .build();
        return Configuration.builder()
                .jsonProvider(new Jackson3JsonProvider(lenient))
                .mappingProvider(new Jackson3MappingProvider(lenient))
                .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
    }
}
