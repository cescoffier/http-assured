package io.smallrye.httpassured.http;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Immutable representation of one part in a multipart HTTP request.
 * <p>
 * Factory methods cover the common upload scenarios: file, raw bytes, and
 * plain text fields. The {@code controlName} corresponds to the HTML form
 * field name ({@code name} attribute in the MIME part header).
 * </p>
 *
 * @param controlName the form-field / control name (MIME part {@code name})
 * @param fileName    the file name advertised in the part header, or {@code null} for text fields
 * @param content     the raw bytes of the part body (never {@code null})
 * @param mimeType    the MIME type of the part, or {@code null} for text fields
 */
public record MultiPartSpec(String controlName, String fileName, byte[] content, String mimeType) {

    public MultiPartSpec {
        Objects.requireNonNull(controlName, "controlName must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }

    /**
     * Creates a file-upload part by reading the contents of {@code file}.
     *
     * @param controlName the form-field name
     * @param file        the file to upload
     * @return a new {@code MultiPartSpec}
     */
    public static MultiPartSpec file(String controlName, File file) {
        Objects.requireNonNull(file, "file must not be null");
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String mime = Files.probeContentType(file.toPath());
            if (mime == null) {
                mime = "application/octet-stream";
            }
            return new MultiPartSpec(controlName, file.getName(), bytes, mime);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + file, e);
        }
    }

    /**
     * Creates a file-upload part using the default control name {@code "file"}.
     *
     * @param file the file to upload
     * @return a new {@code MultiPartSpec}
     */
    public static MultiPartSpec file(File file) {
        return file("file", file);
    }

    /**
     * Creates a binary-upload part from a byte array.
     *
     * @param controlName the form-field name
     * @param fileName    the file name advertised in the part header
     * @param content     the raw bytes
     * @param mimeType    the MIME type
     * @return a new {@code MultiPartSpec}
     */
    public static MultiPartSpec bytes(String controlName, String fileName, byte[] content, String mimeType) {
        return new MultiPartSpec(controlName, fileName, content, mimeType);
    }

    /**
     * Creates a plain-text form field in a multipart request.
     *
     * @param controlName the form-field name
     * @param content     the text value
     * @return a new {@code MultiPartSpec}
     */
    public static MultiPartSpec text(String controlName, String content) {
        return new MultiPartSpec(controlName, null,
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8), null);
    }

    /**
     * Returns {@code true} when this part represents a text field
     * (no file name, no explicit MIME type).
     */
    public boolean isTextField() {
        return fileName == null && mimeType == null;
    }
}
