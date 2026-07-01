package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.Cookie;
import io.smallrye.httpassured.http.Headers;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class ResponseSnippets {

    HttpAssured client = HttpAssured.builder()
            .baseUri("http://localhost:8080").build();

    public void statusCode() {
// tag::status-code[]
client.when().get("/users")
    .then()
        .statusCode(200);
// end::status-code[]
    }

    public void statusLine() {
// tag::status-line[]
client.when().get("/users")
    .then()
        .statusLine("200 OK");
// end::status-line[]
    }

    public void bodyAssertions() {
// tag::body-assertion[]
client.when().get("/users/1")
    .then()
        .body("name", Assertions.isEqualTo("Alice"))
        .body("age", Assertions.greaterThan(18))
        .body("email", Assertions.containsString("@"));
// end::body-assertion[]
    }

    public void multiPathAssertions() {
// tag::multi-path[]
client.when().get("/users/1")
    .then()
        .body("name", Assertions.isEqualTo("Alice"),
              "age", Assertions.greaterThan(18));
// end::multi-path[]
    }

    public void wholeBodyAssertions() {
// tag::body-contains[]
client.when().get("/health")
    .then()
        .bodyContains("UP");
// end::body-contains[]

// tag::body-equals[]
client.when().get("/ping")
    .then()
        .bodyEquals("pong");
// end::body-equals[]
    }

    public void jsonSchema() {
// tag::json-schema-classpath[]
client.when().get("/users/1")
    .then()
        .matchesJsonSchema("schemas/user-schema.json");
// end::json-schema-classpath[]

// tag::json-schema-string[]
String schema = """
        {
          "type": "object",
          "required": ["name", "email"]
        }
        """;
client.when().get("/users/1")
    .then()
        .matchesJsonSchemaString(schema);
// end::json-schema-string[]
    }

    public void extract() {
// tag::extract-path[]
String name = client.when().get("/users/1")
    .then()
        .statusCode(200)
        .extract("name");
// end::extract-path[]

// tag::extract-as-class[]
record User(String name, String email) {}
User user = client.when().get("/users/1")
    .then()
        .statusCode(200)
        .extractAs(User.class);
// end::extract-as-class[]

// tag::extract-as-type-ref[]
List<String> names = client.when().get("/users")
    .then()
        .statusCode(200)
        .extractAs(new TypeReference<List<String>>() {});
// end::extract-as-type-ref[]
    }

    public void responseObject() {
// tag::body-as-string[]
Response response = client.when().get("/users/1");
String body = response.bodyAsString();
// end::body-as-string[]

// tag::body-as-bytes[]
byte[] bytes = client.when().get("/file").bodyAsBytes();
// end::body-as-bytes[]

// tag::pretty-string[]
String pretty = client.when().get("/users/1").asPrettyString();
// end::pretty-string[]

// tag::pretty-print[]
client.when().get("/users/1").prettyPrint();
// end::pretty-print[]

// tag::peek[]
client.when().get("/users/1")
        .peek()
        .then()
        .statusCode(200);
// end::peek[]

// tag::pretty-peek[]
client.when().get("/users/1")
        .prettyPeek()
        .then()
        .statusCode(200);
// end::pretty-peek[]
    }

    public void responseTime() {
// tag::time[]
long ms = client.when().get("/users").time();
// end::time[]

// tag::time-in[]
long seconds = client.when().get("/users").timeIn(TimeUnit.SECONDS);
// end::time-in[]
    }

    public void responseHeaders() {
// tag::get-header[]
String contentType = client.when().get("/users").getHeader("Content-Type");
// end::get-header[]

// tag::headers-object[]
Headers headers = client.when().get("/users").headers();
Optional<String> value = headers.getValue("X-Request-Id");
List<String> allValues = headers.getValues("Set-Cookie");
// end::headers-object[]
    }

    public void detailedCookie() {
// tag::detailed-cookie[]
Cookie cookie = client.when().get("/login").detailedCookie("session");
String value = cookie.value();
String domain = cookie.domain();
String path = cookie.path();
long maxAge = cookie.maxAge();
boolean secure = cookie.isSecured();
boolean httpOnly = cookie.isHttpOnly();
String sameSite = cookie.sameSite();
// end::detailed-cookie[]
    }

    public void rootPath() {
// tag::root-path[]
client.when().get("/store")
    .then()
        .rootPath("store")
        .body("name", Assertions.isEqualTo("My Store"))
        .body("location", Assertions.isNotNull());
// end::root-path[]

// tag::append-root[]
client.when().get("/store")
    .then()
        .rootPath("store")
        .body("name", Assertions.isEqualTo("My Store"))
        .appendRootPath("books[0]")
        .body("title", Assertions.isNotNull())
        .detachRootPath()
        .body("store.location", Assertions.isNotNull());
// end::append-root[]
    }

    public void onFailMessage() {
// tag::on-fail-message[]
client.when().get("/users/1")
    .then()
        .onFailMessage("User 1 should exist and be active")
        .statusCode(200)
        .body("active", Assertions.isEqualTo(true));
// end::on-fail-message[]
    }

    public void chainingSugar() {
// tag::chaining[]
client.when().get("/users/1")
    .then()
        .assertThat()
        .statusCode(200)
        .and()
        .body("name", Assertions.isNotNull());
// end::chaining[]
    }

    public void headerAssertions() {
// tag::assert-header[]
client.when().get("/users")
    .then()
        .header("Content-Type", "application/json")
        .headerExists("X-Request-Id")
        .headerContains("Content-Type", "json");
// end::assert-header[]

// tag::assert-headers-variadic[]
client.when().get("/users")
    .then()
        .headers("Content-Type", "application/json",
                 "X-Request-Id", "req-001");
// end::assert-headers-variadic[]
    }

    public void contentTypeAssertion() {
// tag::assert-content-type[]
client.when().get("/users")
    .then()
        .contentType("application/json");
// end::assert-content-type[]
    }

    public void cookieAssertions() {
// tag::assert-cookie[]
client.when().get("/login")
    .then()
        .cookie("session", "abc123")
        .cookieExists("session");
// end::assert-cookie[]
    }
}
