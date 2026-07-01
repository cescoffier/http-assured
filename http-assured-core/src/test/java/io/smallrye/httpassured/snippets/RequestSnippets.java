package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.http.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

@SuppressWarnings("unused")
public class RequestSnippets {

    HttpAssured client = HttpAssured.builder()
            .baseUri("http://localhost:8080").build();

    public void httpMethods() {
// tag::get[]
client.when().get("/users");
// end::get[]

// tag::post[]
client.given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"Alice\"}")
    .when()
        .post("/users");
// end::post[]

// tag::put[]
client.given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"Alice Updated\"}")
    .when()
        .put("/users/1");
// end::put[]

// tag::patch[]
client.given()
        .contentType(ContentType.JSON)
        .body("{\"email\": \"new@example.com\"}")
    .when()
        .patch("/users/1");
// end::patch[]

// tag::delete[]
client.when().delete("/users/1");
// end::delete[]

// tag::head[]
client.when().head("/users")
    .then()
        .statusCode(200)
        .headerExists("Content-Length");
// end::head[]

// tag::options[]
client.when().options("/users")
    .then()
        .headerContains("Allow", "GET");
// end::options[]
    }

    public void queryParams() {
// tag::query-single[]
client.given()
        .queryParam("status", "active")
    .when()
        .get("/users");
// end::query-single[]

// tag::query-multi-value[]
client.given()
        .queryParam("role", "admin", "editor")
    .when()
        .get("/users");
// end::query-multi-value[]

// tag::query-no-value[]
client.given()
        .queryParam("verbose")
    .when()
        .get("/debug");
// end::query-no-value[]
    }

    public void pathParams() {
// tag::path-param-named[]
client.given()
        .pathParam("id", "42")
    .when()
        .get("/users/{id}");
// end::path-param-named[]

// tag::path-param-positional[]
client.when().get("/users/{id}", 42);
// end::path-param-positional[]
    }

    public void headers() {
// tag::header-single[]
client.given()
        .header("X-Request-Id", "abc-123")
    .when()
        .get("/users");
// end::header-single[]

// tag::header-multi-value[]
client.given()
        .header("Cache-Control", "no-cache", "no-store")
    .when()
        .get("/users");
// end::header-multi-value[]
    }

    public void cookies() {
// tag::cookie[]
client.given()
        .cookie("session", "abc123")
    .when()
        .get("/dashboard");
// end::cookie[]
    }

    public void contentType() {
// tag::content-type-enum[]
client.given()
        .contentType(ContentType.JSON)
        .body("{\"key\": \"value\"}")
    .when()
        .post("/data");
// end::content-type-enum[]
    }

    public void requestBody() {
// tag::body-string[]
client.given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"Alice\"}")
    .when()
        .post("/users");
// end::body-string[]

// tag::body-map[]
client.given()
        .body(Map.of("name", "Alice", "email", "alice@example.com"))
    .when()
        .post("/users");
// end::body-map[]

// tag::body-object[]
record User(String name, String email) {}
client.given()
        .body(new User("Alice", "alice@example.com"))
    .when()
        .post("/users");
// end::body-object[]

// tag::body-inputstream[]
InputStream stream = new ByteArrayInputStream("{\"key\":\"value\"}".getBytes());
client.given()
        .body(stream)
    .when()
        .post("/data");
// end::body-inputstream[]

// tag::body-bytes[]
byte[] data = "{\"key\":\"value\"}".getBytes();
client.given()
        .body(data)
    .when()
        .post("/data");
// end::body-bytes[]
    }

    public void formParams() {
// tag::form-param[]
client.given()
        .formParam("username", "alice")
        .formParam("password", "secret")
    .when()
        .post("/login");
// end::form-param[]

// tag::form-params-map[]
client.given()
        .formParams(Map.of("username", "alice", "password", "secret"))
    .when()
        .post("/login");
// end::form-params-map[]
    }

    public void multipart() {
// tag::multipart-file[]
client.given()
        .multiPart("document", new File("report.pdf"))
    .when()
        .post("/upload");
// end::multipart-file[]

// tag::multipart-bytes[]
byte[] imageData = new byte[]{/* image bytes */};
client.given()
        .multiPart("avatar", "photo.png", imageData, "image/png")
    .when()
        .post("/upload");
// end::multipart-bytes[]

// tag::multipart-text[]
client.given()
        .multiPart("description", "My document")
        .multiPart("document", new File("report.pdf"))
    .when()
        .post("/upload");
// end::multipart-text[]
    }

    public void accept() {
// tag::accept[]
client.given()
        .accept("application/json")
    .when()
        .get("/users");
// end::accept[]

// tag::accept-enum[]
client.given()
        .accept(ContentType.JSON)
    .when()
        .get("/users");
// end::accept-enum[]
    }

    public void urlEncoding() {
// tag::url-encoding-off[]
client.given()
        .urlEncodingEnabled(false)
        .queryParam("q", "hello world")
    .when()
        .get("/search");
// end::url-encoding-off[]
    }

    public void genericRequest() {
// tag::request-enum[]
client.given()
    .when()
        .request(HttpMethod.GET, "/users");
// end::request-enum[]

// tag::request-string[]
client.given()
    .when()
        .request("POST", "/users");
// end::request-string[]
    }
}
