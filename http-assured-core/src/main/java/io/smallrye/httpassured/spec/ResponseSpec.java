package io.smallrye.httpassured.spec;

import io.smallrye.httpassured.assertion.BodyAssertion;
import io.smallrye.httpassured.assertion.JsonSchemaOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A reusable response specification.
 * <p>
 * Captures expected response conditions (status code, headers, body assertions)
 * that can be applied to multiple responses via {@code then().spec(responseSpec)}.
 * </p>
 * <pre>{@code
 * ResponseSpec jsonOk = ResponseSpec.builder()
 *     .statusCode(200)
 *     .headerEquals("Content-Type", "application/json")
 *     .build();
 *
 * client.given().when().get("/users").then().spec(jsonOk);
 * }</pre>
 */
public final class ResponseSpec {

    private final Integer expectedStatusCode;
    private final List<HeaderExpectation> headerExpectations;
    private final List<BodyExpectation<?>> bodyExpectations;
    private final List<JsonSchemaExpectation> jsonSchemaExpectations;

    private ResponseSpec(Builder builder) {
        this.expectedStatusCode = builder.expectedStatusCode;
        this.headerExpectations = Collections.unmodifiableList(builder.headerExpectations);
        this.bodyExpectations = Collections.unmodifiableList(builder.bodyExpectations);
        this.jsonSchemaExpectations = Collections.unmodifiableList(builder.jsonSchemaExpectations);
    }

    public Integer expectedStatusCode() {
        return expectedStatusCode;
    }

    public List<HeaderExpectation> headerExpectations() {
        return headerExpectations;
    }

    public List<BodyExpectation<?>> bodyExpectations() {
        return bodyExpectations;
    }

    /** JSON Schema expectations registered via {@link Builder#jsonSchema(String)}. */
    public List<JsonSchemaExpectation> jsonSchemaExpectations() {
        return jsonSchemaExpectations;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A header expectation: header name and expected value.
     */
    public record HeaderExpectation(String name, String expectedValue) {}

    /**
     * A body expectation: JsonPath and assertion.
     */
    public record BodyExpectation<T>(String path, BodyAssertion<T> assertion) {}

    /**
     * A JSON Schema expectation: classpath resource and options.
     */
    public record JsonSchemaExpectation(String classpathResource, JsonSchemaOptions opts) {}

    public static final class Builder {
        private Integer expectedStatusCode;
        private final List<HeaderExpectation> headerExpectations = new ArrayList<>();
        private final List<BodyExpectation<?>> bodyExpectations = new ArrayList<>();
        private final List<JsonSchemaExpectation> jsonSchemaExpectations = new ArrayList<>();

        private Builder() {}

        public Builder statusCode(int statusCode) {
            this.expectedStatusCode = statusCode;
            return this;
        }

        public Builder headerEquals(String name, String expectedValue) {
            this.headerExpectations.add(new HeaderExpectation(name, expectedValue));
            return this;
        }

        public <T> Builder body(String path, BodyAssertion<T> assertion) {
            this.bodyExpectations.add(new BodyExpectation<>(path, assertion));
            return this;
        }

        /**
         * Adds a whole-body JSON Schema assertion using a classpath resource.
         *
         * <pre>{@code
         * ResponseSpec spec = ResponseSpec.builder()
         *     .statusCode(200)
         *     .jsonSchema("schemas/user.json")
         *     .build();
         * }</pre>
         *
         * @param classpathResource path to the JSON Schema file on the classpath
         */
        public Builder jsonSchema(String classpathResource) {
            this.jsonSchemaExpectations.add(new JsonSchemaExpectation(classpathResource, null));
            return this;
        }

        /**
         * Adds a whole-body JSON Schema assertion using a classpath resource and options.
         *
         * @param classpathResource path to the JSON Schema file on the classpath
         * @param opts              schema version and fail-fast options
         */
        public Builder jsonSchema(String classpathResource, JsonSchemaOptions opts) {
            this.jsonSchemaExpectations.add(new JsonSchemaExpectation(classpathResource, opts));
            return this;
        }

        public ResponseSpec build() {
            return new ResponseSpec(this);
        }
    }
}
