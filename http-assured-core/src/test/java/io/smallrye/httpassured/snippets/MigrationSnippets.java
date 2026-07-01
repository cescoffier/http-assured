package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.dsl.Response;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.spec.RequestSpec;

@SuppressWarnings("unused")
public class MigrationSnippets {

    public void afterComplete() {
// tag::after-complete[]
HttpAssured client = HttpAssured.builder()
        .baseUri("http://localhost")
        .port(8080)
        .basePath("/api")
        .defaultHeader("Accept", "application/json")
        .build();

client.given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"Alice\"}")
    .when()
        .post("/users")
    .then()
        .statusCode(201)
        .body("name", Assertions.isEqualTo("Alice"))
        .body("id", Assertions.isNotNull());
// end::after-complete[]
    }

    public void afterBuilder() {
// tag::after-builder[]
HttpAssured client = HttpAssured.builder()
        .baseUri("http://localhost")
        .port(8080)
        .basePath("/api")
        .build();
// end::after-builder[]
    }

    public void afterAssertions() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::after-assertions[]
client.when().get("/users/1")
    .then()
        .body("name", Assertions.isEqualTo("Alice"))
        .body("tags", Assertions.containsAll("admin", "user"))
        .body("tags", Assertions.hasSize(2))
        .body("bio", Assertions.containsString("engineer"))
        .body("age", Assertions.greaterThan(18))
        .body("deleted", Assertions.isNull());
// end::after-assertions[]
    }

    public void afterExtract() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::after-extract[]
String id = client.given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"Alice\"}")
    .when()
        .post("/users")
    .then()
        .statusCode(201)
        .extract("id");
// end::after-extract[]
    }

    public void afterSpec() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::after-spec[]
RequestSpec bearerSpec = RequestSpec.builder()
        .header("Authorization", "Bearer my-token")
        .accept(ContentType.JSON)
        .build();

client.given()
        .spec(bearerSpec)
    .when()
        .get("/protected")
    .then()
        .statusCode(200);
// end::after-spec[]
    }

    public void afterResponse() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::after-response[]
Response response = client.when().get("/users/1");
String body = response.bodyAsString();
String header = response.getHeader("Content-Type");
// end::after-response[]
    }
}
