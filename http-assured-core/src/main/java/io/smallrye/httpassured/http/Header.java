package io.smallrye.httpassured.http;

import java.util.Objects;

/**
 * An HTTP header name-value pair.
 */
public record Header(String name, String value) {

    public Header {
        Objects.requireNonNull(name, "Header name must not be null");
        Objects.requireNonNull(value, "Header value must not be null");
    }

    /**
     * Checks if this header has the same name as the other header (case-insensitive).
     *
     * @param other the other header to compare names with
     * @return true if the header names match (ignoring case)
     */
    public boolean hasSameNameAs(Header other) {
        if (other == null) {
            return false;
        }
        return name.equalsIgnoreCase(other.name);
    }
}
