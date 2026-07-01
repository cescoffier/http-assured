package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.spec.RequestSpec;
import io.smallrye.httpassured.spec.ResponseSpec;

import java.time.Duration;

/**
 * Code snippets for the Specifications & Configuration guide.
 * This class must compile — it validates that all documented API usage is correct.
 * Methods are not executed as tests; they exist solely for compilation checks.
 */
@SuppressWarnings("unused")
public class SpecSnippets {

    HttpAssured client = HttpAssured.builder()
            .baseUri("http://localhost:8080").build();

    public void requestSpec() {
// tag::request-spec-build[]
RequestSpec jsonApi = RequestSpec.builder()
        .header("Authorization", "Bearer my-token")
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .build();
// end::request-spec-build[]

// tag::request-spec-use[]
client.given()
        .spec(jsonApi)
        .body("{\"name\": \"Alice\"}")
    .when()
        .post("/users")
    .then()
        .statusCode(201);
// end::request-spec-use[]
    }

    public void responseSpec() {
// tag::response-spec-build[]
ResponseSpec successfulJson = ResponseSpec.builder()
        .statusCode(200)
        .headerEquals("Content-Type", "application/json")
        .body("status", Assertions.isEqualTo("ok"))
        .build();
// end::response-spec-build[]

// tag::response-spec-use[]
client.when().get("/health")
    .then()
        .spec(successfulJson);
// end::response-spec-use[]
    }

    public void clientConfig() {
// tag::client-config[]
HttpAssured client = HttpAssured.builder()
        .baseUri("http://localhost")
        .port(8080)
        .basePath("/api/v1")
        .defaultHeader("Accept", "application/json")
        .requestTimeout(Duration.ofSeconds(30))
        .build();
// end::client-config[]
    }

    public void redirectControl() {
// tag::redirect-dont-follow[]
client.given()
        .redirects().follow(false)
    .when()
        .get("/old-page")
    .then()
        .statusCode(302);
// end::redirect-dont-follow[]

// tag::redirect-max[]
client.given()
        .redirects().max(3)
    .when()
        .get("/redirect-chain");
// end::redirect-max[]
    }
}
