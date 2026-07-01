package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;

@SuppressWarnings("unused")
public class LoggingSnippets {

    HttpAssured client = HttpAssured.builder()
            .baseUri("http://localhost:8080").build();

    public void requestLogging() {
// tag::request-all[]
client.given()
        .log().all()
    .when()
        .get("/users");
// end::request-all[]

// tag::request-headers[]
client.given()
        .log().headers()
    .when()
        .get("/users");
// end::request-headers[]

// tag::request-body[]
client.given()
        .log().body()
    .when()
        .post("/users");
// end::request-body[]
    }

    public void responseLogging() {
// tag::response-all[]
client.when().get("/users")
    .then()
        .log().all()
        .statusCode(200);
// end::response-all[]

// tag::response-headers[]
client.when().get("/users")
    .then()
        .log().headers()
        .statusCode(200);
// end::response-headers[]

// tag::response-body[]
client.when().get("/users")
    .then()
        .log().body()
        .statusCode(200);
// end::response-body[]
    }

    public void conditionalLogging() {
// tag::if-validation-fails[]
client.given()
        .log().all()
    .when()
        .get("/users")
    .then()
        .log().ifValidationFails()
        .statusCode(200);
// end::if-validation-fails[]

// tag::if-status-code[]
client.when().get("/users")
    .then()
        .log().ifStatusCodeIsEqualTo(500)
        .statusCode(200);
// end::if-status-code[]

// tag::if-error[]
client.when().get("/users")
    .then()
        .log().ifError()
        .statusCode(200);
// end::if-error[]

// tag::if-status-matches[]
client.when().get("/users")
    .then()
        .log().ifStatusCodeMatches(code -> code >= 400)
        .statusCode(200);
// end::if-status-matches[]
    }

    public void builderLogging() {
// tag::builder-log[]
HttpAssured loggingClient = HttpAssured.builder()
        .baseUri("http://localhost:8080")
        .logIfValidationFails()
        .build();
// end::builder-log[]
    }

    public void blacklistHeaders() {
// tag::blacklist[]
HttpAssured secureClient = HttpAssured.builder()
        .baseUri("http://localhost:8080")
        .blacklistHeaders("Authorization", "Cookie", "X-Api-Key")
        .build();
// end::blacklist[]
    }
}
