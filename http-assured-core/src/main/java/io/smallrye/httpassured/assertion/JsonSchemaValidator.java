package io.smallrye.httpassured.assertion;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Internal helper that bridges http-assured's assertion model to the
 * {@code com.networknt:json-schema-validator} library.
 * <p>
 * All public methods return a {@link BodyAssertion}{@code <String>} that accepts
 * a raw JSON string and throws {@link AssertionError} when the JSON does not
 * conform to the schema.
 * </p>
 * <p>
 * The networknt library is an <em>optional</em> dependency. If it is absent from
 * the classpath every method in this class throws {@link IllegalStateException}
 * with an actionable message, rather than a {@link ClassNotFoundException} at
 * unexpected call sites.
 * </p>
 */
public final class JsonSchemaValidator {

    private static final String NETWORKNT_FACTORY = "com.networknt.schema.JsonSchemaFactory";

    private JsonSchemaValidator() {}

    /**
     * Creates an assertion that validates against a JSON schema loaded from the
     * classpath.
     *
     * @param classpathResource resource path (e.g. {@code "schemas/user.json"})
     * @param opts              validation options; {@code null} uses defaults
     */
    public static BodyAssertion<Object> fromClasspath(String classpathResource, JsonSchemaOptions opts) {
        checkAvailable();
        return (body, path) -> {
            String bodyJson = toJsonString(body);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String resource = classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;
            InputStream stream = cl.getResourceAsStream(classpathResource.startsWith("/")
                    ? classpathResource.substring(1) : classpathResource);
            if (stream == null) {
                // try with leading slash via Class resource lookup
                stream = JsonSchemaValidator.class.getResourceAsStream(resource);
            }
            if (stream == null) {
                fail("JSON Schema classpath resource not found: " + classpathResource);
                return;
            }
            try (InputStream s = stream) {
                validateStream(s, bodyJson, opts, "classpath:" + classpathResource, path);
            } catch (IOException e) {
                fail("Failed to read JSON Schema from classpath '" + classpathResource + "': " + e.getMessage());
            }
        };
    }

    /**
     * Creates an assertion that validates against a JSON schema provided as a
     * {@link String}.
     *
     * @param schemaJson the schema JSON string
     * @param opts       validation options; {@code null} uses defaults
     */
    public static BodyAssertion<Object> fromString(String schemaJson, JsonSchemaOptions opts) {
        checkAvailable();
        return (body, path) -> {
            String bodyJson = toJsonString(body);
            try {
                com.networknt.schema.JsonSchemaFactory factory = resolveFactory(opts);
                com.networknt.schema.JsonSchema schema = factory.getSchema(schemaJson);
                validate(schema, bodyJson, opts, "inline schema", path);
            } catch (AssertionError ae) {
                throw ae;
            } catch (Exception e) {
                fail("JSON Schema validation error: " + e.getMessage());
            }
        };
    }

    /**
     * Creates an assertion that validates against a JSON schema loaded from the
     * given {@link InputStream}.
     *
     * @param schemaStream an open stream providing the schema JSON
     * @param opts         validation options; {@code null} uses defaults
     */
    public static BodyAssertion<Object> fromStream(InputStream schemaStream, JsonSchemaOptions opts) {
        checkAvailable();
        return (body, path) -> {
            String bodyJson = toJsonString(body);
            try {
                validateStream(schemaStream, bodyJson, opts, "InputStream", path);
            } catch (IOException e) {
                fail("Failed to read JSON Schema from InputStream: " + e.getMessage());
            }
        };
    }

    /**
     * Creates an assertion that validates against a JSON schema loaded from the
     * given {@link URI}.
     *
     * @param schemaUri the schema URI (classpath, file, http, …)
     * @param opts      validation options; {@code null} uses defaults
     */
    public static BodyAssertion<Object> fromUri(URI schemaUri, JsonSchemaOptions opts) {
        checkAvailable();
        return (body, path) -> {
            String bodyJson = toJsonString(body);
            try {
                com.networknt.schema.JsonSchemaFactory factory = resolveFactory(opts);
                com.networknt.schema.JsonSchema schema = factory.getSchema(schemaUri);
                validate(schema, bodyJson, opts, schemaUri.toString(), path);
            } catch (AssertionError ae) {
                throw ae;
            } catch (Exception e) {
                fail("JSON Schema validation error for URI '" + schemaUri + "': " + e.getMessage());
            }
        };
    }

    private static void validateStream(InputStream stream, String body, JsonSchemaOptions opts,
                                        String schemaLabel, String path) throws IOException {
        com.networknt.schema.JsonSchemaFactory factory = resolveFactory(opts);
        com.networknt.schema.JsonSchema schema = factory.getSchema(stream);
        validate(schema, body, opts, schemaLabel, path);
    }

    /**
     * Serializes an arbitrary value to a JSON string if it is not already one.
     * This is needed when the value comes from a JsonPath extraction (e.g. a
     * {@link java.util.Map} or {@link java.util.List}) rather than a raw body string.
     */
    static String toJsonString(Object value) {
        if (value instanceof String s) {
            return s;
        }
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static void validate(com.networknt.schema.JsonSchema schema, String bodyJson,
                                  JsonSchemaOptions opts, String schemaLabel, String path) {
        boolean failFast = opts != null && opts.isFailFast();
        Set<com.networknt.schema.ValidationMessage> errors;
        if (failFast) {
            errors = schema.validate(bodyJson, com.networknt.schema.InputFormat.JSON,
                    ctx -> ctx.getExecutionConfig().setFailFast(true));
        } else {
            errors = schema.validate(bodyJson, com.networknt.schema.InputFormat.JSON);
        }
        if (!errors.isEmpty()) {
            String violations = errors.stream()
                    .map(com.networknt.schema.ValidationMessage::getMessage)
                    .sorted()
                    .collect(Collectors.joining("\n  - ", "\n  - ", ""));
            String location = (path != null && !path.isEmpty() && !"<root>".equals(path))
                    ? " at '" + path + "'" : "";
            fail("JSON Schema validation failed" + location + " against '" + schemaLabel + "':" + violations);
        }
    }

    private static com.networknt.schema.JsonSchemaFactory resolveFactory(JsonSchemaOptions opts) {
        com.networknt.schema.SpecVersion.VersionFlag flag = toVersionFlag(
                opts != null ? opts.version() : JsonSchemaOptions.Version.DRAFT_7);
        return com.networknt.schema.JsonSchemaFactory.getInstance(flag);
    }

    private static com.networknt.schema.SpecVersion.VersionFlag toVersionFlag(JsonSchemaOptions.Version v) {
        return switch (v) {
            case DRAFT_4      -> com.networknt.schema.SpecVersion.VersionFlag.V4;
            case DRAFT_6      -> com.networknt.schema.SpecVersion.VersionFlag.V6;
            case DRAFT_7      -> com.networknt.schema.SpecVersion.VersionFlag.V7;
            case DRAFT_2019_09 -> com.networknt.schema.SpecVersion.VersionFlag.V201909;
            case DRAFT_2020_12 -> com.networknt.schema.SpecVersion.VersionFlag.V202012;
        };
    }

    /** Guards against missing optional dependency with an actionable error message. */
    public static void checkAvailable() {
        try {
            Class.forName(NETWORKNT_FACTORY);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "com.networknt:json-schema-validator is not on the classpath. " +
                    "Add it as a test dependency to use matchesJsonSchema() assertions.", e);
        }
    }
}
