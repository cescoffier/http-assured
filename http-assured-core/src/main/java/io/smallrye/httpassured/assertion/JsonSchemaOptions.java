package io.smallrye.httpassured.assertion;

/**
 * Configuration options for JSON Schema validation.
 * <p>
 * Controls the JSON Schema draft version and fail-fast behaviour.
 * Use the static factory methods to obtain a pre-configured instance:
 * </p>
 * <pre>{@code
 * // Default: Draft 7
 * .matchesJsonSchema("schemas/user.json")
 *
 * // Explicit draft version
 * .matchesJsonSchema("schemas/user.json", JsonSchemaOptions.draft2020())
 *
 * // Draft 7 + stop after the first violation
 * .matchesJsonSchema("schemas/user.json", JsonSchemaOptions.draft7().failFast())
 * }</pre>
 */
public final class JsonSchemaOptions {

    /**
     * JSON Schema draft version identifiers.
     * <p>
     * These mirror {@code com.networknt.schema.SpecVersion.VersionFlag} but avoid
     * a compile-time dependency on the optional library in consuming code.
     * </p>
     */
    public enum Version {
        /** JSON Schema Draft 4 */
        DRAFT_4,
        /** JSON Schema Draft 6 */
        DRAFT_6,
        /** JSON Schema Draft 7 (default) */
        DRAFT_7,
        /** JSON Schema Draft 2019-09 */
        DRAFT_2019_09,
        /** JSON Schema Draft 2020-12 */
        DRAFT_2020_12
    }

    private final Version version;
    private final boolean failFast;

    private JsonSchemaOptions(Version version, boolean failFast) {
        this.version = version;
        this.failFast = failFast;
    }

    /** Default options: Draft 7, all violations reported. */
    public static JsonSchemaOptions defaults() {
        return new JsonSchemaOptions(Version.DRAFT_7, false);
    }

    /** Draft 4 options. */
    public static JsonSchemaOptions draft4() {
        return new JsonSchemaOptions(Version.DRAFT_4, false);
    }

    /** Draft 6 options. */
    public static JsonSchemaOptions draft6() {
        return new JsonSchemaOptions(Version.DRAFT_6, false);
    }

    /** Draft 7 options (same as {@link #defaults()}). */
    public static JsonSchemaOptions draft7() {
        return new JsonSchemaOptions(Version.DRAFT_7, false);
    }

    /** Draft 2019-09 options. */
    public static JsonSchemaOptions draft2019() {
        return new JsonSchemaOptions(Version.DRAFT_2019_09, false);
    }

    /** Draft 2020-12 options. */
    public static JsonSchemaOptions draft2020() {
        return new JsonSchemaOptions(Version.DRAFT_2020_12, false);
    }

    /** Returns a copy of these options with fail-fast enabled (stop after the first violation). */
    public JsonSchemaOptions failFast() {
        return new JsonSchemaOptions(this.version, true);
    }

    Version version() {
        return version;
    }

    boolean isFailFast() {
        return failFast;
    }
}
