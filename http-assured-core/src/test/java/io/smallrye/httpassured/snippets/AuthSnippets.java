package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.spi.TrustOptions;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class AuthSnippets {

    HttpAssured client = HttpAssured.builder()
            .baseUri("http://localhost:8080").build();

    public void basicAuth() {
// tag::basic[]
client.given()
        .auth().basic("user", "secret")
    .when()
        .get("/secure")
    .then()
        .statusCode(200);
// end::basic[]
    }

    public void preemptiveBasic() {
// tag::preemptive[]
client.given()
        .auth().preemptive().basic("user", "secret")
    .when()
        .get("/secure")
    .then()
        .statusCode(200);
// end::preemptive[]
    }

    public void oauth2() {
// tag::oauth2[]
client.given()
        .auth().oauth2("my-access-token")
    .when()
        .get("/api/resource")
    .then()
        .statusCode(200);
// end::oauth2[]
    }

    public void oauth1() {
// tag::oauth1[]
client.given()
        .auth().oauth("consumerKey", "consumerSecret",
                      "accessToken", "tokenSecret")
    .when()
        .get("/api/resource")
    .then()
        .statusCode(200);
// end::oauth1[]
    }

    public void trustAll() {
// tag::trust-all[]
client.given()
        .trustAll(true)
    .when()
        .get("/secure-endpoint")
    .then()
        .statusCode(200);
// end::trust-all[]
    }

    public void trustOptions() {
// tag::trust-pem[]
client.given()
        .trustOptions(TrustOptions.pem(Path.of("certs/ca.pem")))
    .when()
        .get("/secure-endpoint")
    .then()
        .statusCode(200);
// end::trust-pem[]

// tag::trust-jks[]
client.given()
        .trustOptions(TrustOptions.jks(Path.of("certs/truststore.jks"), "changeit"))
    .when()
        .get("/secure-endpoint")
    .then()
        .statusCode(200);
// end::trust-jks[]

// tag::trust-pkcs12[]
client.given()
        .trustOptions(TrustOptions.pkcs12(Path.of("certs/truststore.p12"), "changeit"))
    .when()
        .get("/secure-endpoint")
    .then()
        .statusCode(200);
// end::trust-pkcs12[]
    }
}
