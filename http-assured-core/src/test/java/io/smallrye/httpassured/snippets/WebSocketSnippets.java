package io.smallrye.httpassured.snippets;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.websocket.WsSession;

import java.time.Duration;

/**
 * Code snippets included in the documentation via AsciiDoc tagged regions.
 * This class must compile — it validates that all documented API usage is correct.
 * Methods are not executed as tests; they exist solely for compilation checks.
 */
@SuppressWarnings("unused")
public class WebSocketSnippets {

    HttpAssured client = HttpAssured.builder()
            .baseUri("http://localhost:8080").build();

    public void openSession() {
// tag::connect[]
WsSession session = client.webSocket("/chat").connect();
// end::connect[]
        session.close();
    }

    public void connectWithOptions() {
// tag::connect-options[]
WsSession session = client.webSocket("/chat")
        .header("Authorization", "Bearer my-token")
        .queryParam("room", "general")
        .connect();
// end::connect-options[]
        session.close();
    }

    public void sendText() {
// tag::send-text[]
try (WsSession session = client.webSocket("/echo").connect()) {
    session.sendText("hello");
    String reply = session.awaitText(Duration.ofSeconds(5));
}
// end::send-text[]
    }

    public void sendBinary() {
// tag::send-binary[]
try (WsSession session = client.webSocket("/echo").connect()) {
    session.sendBinary(new byte[]{1, 2, 3});
    byte[] reply = session.awaitBinary(Duration.ofSeconds(5));
}
// end::send-binary[]
    }

    public void closeSession() {
// tag::close[]
WsSession session = client.webSocket("/chat").connect();
session.close();
// end::close[]
    }

    public void closeWithCode() {
// tag::close-code[]
WsSession session = client.webSocket("/chat").connect();
session.close(1000, "normal closure");
// end::close-code[]
    }

    public void checkOpen() {
// tag::is-open[]
WsSession session = client.webSocket("/chat").connect();
boolean open = session.isOpen();
session.close();
// end::is-open[]
    }

    public void tryWithResources() {
// tag::try-with-resources[]
try (WsSession session = client.webSocket("/echo").connect()) {
    session.sendText("ping");
    String pong = session.awaitText(Duration.ofSeconds(5));
}
// end::try-with-resources[]
    }
}
