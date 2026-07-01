package io.smallrye.httpassured.spi;

import io.smallrye.httpassured.websocket.WsSession;
import io.smallrye.mutiny.Uni;

/**
 * SPI for HTTP client implementations.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 * The default implementation uses Vert.x WebClient.
 * </p>
 * <p>
 * Engines must be thread-safe and reusable across multiple requests.
 * </p>
 */
public interface HttpClientEngine extends AutoCloseable {

    /**
     * Executes an HTTP request synchronously.
     *
     * @param request the request context
     * @return the raw HTTP response
     * @throws io.smallrye.httpassured.HttpAssuredException if the request fails
     */
    RawResponse execute(RequestContext request);

    /**
     * Executes an HTTP request asynchronously using Mutiny.
     *
     * @param request the request context
     * @return a Uni emitting the raw HTTP response
     */
    Uni<RawResponse> executeAsync(RequestContext request);

    /**
     * Opens a WebSocket connection.
     *
     * @param context the WebSocket connection context
     * @return a Uni emitting the WebSocket session
     */
    Uni<WsSession> openWebSocket(WebSocketContext context);

    /**
     * Closes the engine and releases all resources.
     */
    @Override
    void close();
}
