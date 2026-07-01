package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * REST Assured compatibility tests -- Multipart upload.
 *
 * <p>Covers file upload, byte-array upload, text fields in multipart forms,
 * automatic Content-Type setting, and the body/multipart conflict guard.
 */
@WireMockTest
class MultiPartUploadTest {

    private HttpAssured client;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setupClient(WireMockRuntimeInfo wmInfo) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .build();
    }

    @AfterEach
    void closeClient() {
        if (client != null) client.close();
    }

    @Test
    void shouldUploadFile() throws IOException {
        stubFor(post(urlEqualTo("/upload"))
                .withMultipartRequestBody(
                        aMultipart()
                                .withName("myFile")
                                .withBody(containing("hello multipart")))
                .willReturn(aResponse().withStatus(200)));

        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "hello multipart");

        client.given()
                .multiPart("myFile", file)
                .when().post("/upload")
                .then()
                .statusCode(200);

        verify(postRequestedFor(urlEqualTo("/upload"))
                .withHeader("Content-Type", containing("multipart/form-data")));
    }

    @Test
    void shouldUploadFileWithDefaultControlName() throws IOException {
        stubFor(post(urlEqualTo("/upload-default"))
                .withMultipartRequestBody(
                        aMultipart()
                                .withName("file")
                                .withBody(containing("default name")))
                .willReturn(aResponse().withStatus(200)));

        File file = tempDir.resolve("default.txt").toFile();
        Files.writeString(file.toPath(), "default name");

        client.given()
                .multiPart(file)
                .when().post("/upload-default")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldUploadBytes() {
        stubFor(post(urlEqualTo("/upload-bytes"))
                .withMultipartRequestBody(
                        aMultipart()
                                .withName("data")
                                .withBody(containing("binary content")))
                .willReturn(aResponse().withStatus(200)));

        byte[] content = "binary content".getBytes(StandardCharsets.UTF_8);

        client.given()
                .multiPart("data", "payload.bin", content, "application/octet-stream")
                .when().post("/upload-bytes")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldUploadTextPart() {
        stubFor(post(urlEqualTo("/upload-text"))
                .withMultipartRequestBody(
                        aMultipart()
                                .withName("description")
                                .withBody(equalTo("some description")))
                .willReturn(aResponse().withStatus(200)));

        client.given()
                .multiPart("description", "some description")
                .when().post("/upload-text")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldUploadMultipleParts() throws IOException {
        stubFor(post(urlEqualTo("/upload-multi"))
                .withMultipartRequestBody(
                        aMultipart().withName("document"))
                .withMultipartRequestBody(
                        aMultipart().withName("label"))
                .willReturn(aResponse().withStatus(200)));

        File file = tempDir.resolve("doc.txt").toFile();
        Files.writeString(file.toPath(), "document content");

        client.given()
                .multiPart("document", file)
                .multiPart("label", "my-document")
                .when().post("/upload-multi")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldAutoSetContentType() throws IOException {
        stubFor(post(urlEqualTo("/upload-auto-ct"))
                .willReturn(aResponse().withStatus(200)));

        File file = tempDir.resolve("auto.txt").toFile();
        Files.writeString(file.toPath(), "auto content type");

        client.given()
                .multiPart(file)
                .when().post("/upload-auto-ct")
                .then()
                .statusCode(200);

        // Verify Content-Type header starts with multipart/form-data
        var requests = findAll(postRequestedFor(urlEqualTo("/upload-auto-ct")));
        assertThat(requests).hasSize(1);
        String ct = requests.get(0).getHeader("Content-Type");
        assertThat(ct).startsWith("multipart/form-data");
    }

    @Test
    void shouldRejectBodyAndMultiPartCombination() throws IOException {
        File file = tempDir.resolve("conflict.txt").toFile();
        Files.writeString(file.toPath(), "conflict");

        assertThrows(IllegalStateException.class, () ->
                client.given()
                        .body("some body")
                        .multiPart(file)
                        .when().post("/should-not-reach"));
    }
}
