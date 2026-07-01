package io.smallrye.httpassured.websocket;

import io.smallrye.mutiny.Multi;

import java.time.Duration;

/**
 * A WebSocket session for sending and receiving messages.
 * <p>
 * Intended to be used with try-with-resources for automatic cleanup:
 * <pre>{@code
 * try (WsSession session = client.webSocket("/chat").connect()) {
 *     session.sendText("hello");
 *     String reply = session.awaitText(Duration.ofSeconds(5));
 *     assertEquals("world", reply);
 * }
 * }</pre>
 * </p>
 */
public interface WsSession extends AutoCloseable {

    /**
     * Sends a text message.
     *
     * @param message the text payload
     * @return this session for chaining
     */
    WsSession sendText(String message);

    /**
     * Sends a binary message.
     *
     * @param data the binary payload
     * @return this session for chaining
     */
    WsSession sendBinary(byte[] data);

    /**
     * Sends a ping frame.
     *
     * @return this session for chaining
     */
    WsSession sendPing();

    /**
     * Blocks until a text message is received or the timeout expires.
     *
     * @param timeout maximum time to wait
     * @return the received text message
     * @throws io.smallrye.httpassured.HttpAssuredException if timeout expires or connection closes
     */
    String awaitText(Duration timeout);

    /**
     * Blocks until a binary message is received or the timeout expires.
     *
     * @param timeout maximum time to wait
     * @return the received binary data
     * @throws io.smallrye.httpassured.HttpAssuredException if timeout expires or connection closes
     */
    byte[] awaitBinary(Duration timeout);

    /**
     * Returns a reactive stream of incoming text messages.
     * The stream completes when the WebSocket connection closes.
     *
     * @return a Multi of text messages
     */
    Multi<String> textMessages();

    /**
     * Returns a reactive stream of incoming binary messages.
     * The stream completes when the WebSocket connection closes.
     *
     * @return a Multi of binary payloads
     */
    Multi<byte[]> binaryMessages();

    /**
     * Returns true if the WebSocket connection is currently open.
     */
    boolean isOpen();

    /**
     * Closes the WebSocket connection gracefully.
     */
    @Override
    void close();

    /**
     * Closes the WebSocket connection with a specific status code and reason.
     *
     * @param statusCode the WebSocket close status code
     * @param reason     the close reason
     */
    void close(int statusCode, String reason);
}
