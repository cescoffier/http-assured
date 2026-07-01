package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.spec.RequestSpec;

/**
 * Code snippets included in the documentation via AsciiDoc tagged regions.
 * This class must compile — it validates that all documented API usage is correct.
 * Methods are not executed as tests; they exist solely for compilation checks.
 */
@SuppressWarnings("unused")
public class GettingStartedSnippets {

    public void createClient() {
// tag::create-client[]
HttpAssured client = HttpAssured.builder()
        .baseUri("http://localhost:8080")
        .defaultHeader("Accept", "application/json")
        .build();
// end::create-client[]
    }

    public void basicGet() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::basic-get[]
client.given()
        .header("Accept", "application/json")
        .when()
        .get("/users")
        .then()
        .statusCode(200)
        .body("name", Assertions.isNotNull());
// end::basic-get[]
    }

    public void postJson() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::post-json[]
client.given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"Alice\", \"email\": \"alice@example.com\"}")
        .when()
        .post("/users")
        .then()
        .statusCode(201);
// end::post-json[]
    }

    public void queryParams() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::query-params[]
client.given()
        .queryParam("page", "1")
        .queryParam("size", "10")
        .when()
        .get("/users")
        .then()
        .statusCode(200);
// end::query-params[]
    }

    public void jsonPath() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::json-path[]
client.given()
        .when()
        .get("/users/1")
        .then()
        .statusCode(200)
        .body("name", Assertions.isNotNull())
        .body("email", Assertions.containsString("@"));
// end::json-path[]
    }

    public void basicAuth() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::basic-auth[]
client.given()
        .auth().basic("user", "secret")
        .when()
        .get("/secure")
        .then()
        .statusCode(200);
// end::basic-auth[]
    }

    public void preemptiveBasicAuth() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::preemptive-basic-auth[]
client.given()
        .auth().preemptive().basic("user", "secret")
        .when()
        .get("/secure")
        .then()
        .statusCode(200);
// end::preemptive-basic-auth[]
    }

    public void oauth2Auth() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::oauth2-auth[]
client.given()
        .auth().oauth2("my-access-token")
        .when()
        .get("/api/resource")
        .then()
        .statusCode(200);
// end::oauth2-auth[]
    }

    public void oauth1Auth() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::oauth1-auth[]
client.given()
        .auth().oauth("consumerKey", "consumerSecret", "accessToken", "tokenSecret")
        .when()
        .get("/api/resource")
        .then()
        .statusCode(200);
// end::oauth1-auth[]
    }

    public void authSpec() {
        HttpAssured client = HttpAssured.builder()
                .baseUri("http://localhost:8080").build();
// tag::auth-spec[]
RequestSpec bearerSpec = RequestSpec.builder()
        .header("Authorization", "Bearer my-token")
        .build();

client.given()
        .spec(bearerSpec)
        .when()
        .get("/api/resource")
        .then()
        .statusCode(200);
// end::auth-spec[]
    }
}
