package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.engine.vertx.VertxHttpEngine;
import io.smallrye.httpassured.toxic.Toxic;
import io.smallrye.httpassured.toxic.ToxicEngine;

import java.time.Duration;
import java.util.List;

@SuppressWarnings("unused")
public class ToxicSnippets {

    HttpAssured client = HttpAssured.builder()
            .baseUri("http://localhost:8080").build();

    public void latency() {
// tag::latency[]
client.given()
        .toxic(Toxic.latency(Duration.ofSeconds(2)))
    .when()
        .get("/api/data")
    .then()
        .statusCode(200);
// end::latency[]
    }

    public void latencyWithJitter() {
// tag::latency-jitter[]
client.given()
        .toxic(Toxic.latency(Duration.ofMillis(500), Duration.ofMillis(200)))
    .when()
        .get("/api/data");
// end::latency-jitter[]
    }

    public void down() {
// tag::down[]
client.given()
        .toxic(Toxic.down())
    .when()
        .get("/api/data");
// end::down[]
    }

    public void timeout() {
// tag::timeout[]
client.given()
        .toxic(Toxic.timeout(Duration.ofSeconds(30)))
    .when()
        .get("/api/data");
// end::timeout[]
    }

    public void resetPeer() {
// tag::reset-peer[]
client.given()
        .toxic(Toxic.resetPeer())
    .when()
        .get("/api/data");
// end::reset-peer[]
    }

    public void bandwidth() {
// tag::bandwidth[]
client.given()
        .toxic(Toxic.bandwidth(1024))
    .when()
        .get("/api/large-payload");
// end::bandwidth[]
    }

    public void respondWith() {
// tag::respond-with[]
client.given()
        .toxic(Toxic.respondWith(503, "{\"error\": \"Service Unavailable\"}"))
    .when()
        .get("/api/data")
    .then()
        .statusCode(503)
        .body("error", Assertions.isEqualTo("Service Unavailable"));
// end::respond-with[]
    }

    public void slowClose() {
// tag::slow-close[]
client.given()
        .toxic(Toxic.slowClose(Duration.ofSeconds(5)))
    .when()
        .get("/api/data");
// end::slow-close[]
    }

    public void limitData() {
// tag::limit-data[]
client.given()
        .toxic(Toxic.limitData(100))
    .when()
        .get("/api/large-payload");
// end::limit-data[]
    }

    public void toxicity() {
// tag::toxicity[]
client.given()
        .toxic(Toxic.latency(Duration.ofSeconds(2)).withToxicity(0.5))
    .when()
        .get("/api/data");
// end::toxicity[]
    }

    public void programmatic() {
// tag::toxic-engine[]
ToxicEngine engine = new ToxicEngine(
        new VertxHttpEngine(),
        List.of(
            Toxic.latency(Duration.ofMillis(200)),
            Toxic.bandwidth(10_000)
        ));

HttpAssured faultyClient = HttpAssured.builder()
        .baseUri("http://localhost:8080")
        .engine(engine)
        .build();
// end::toxic-engine[]
    }
}
