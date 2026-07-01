package io.smallrye.httpassured.websocket;

import io.smallrye.httpassured.config.HttpAssuredConfig;
import io.smallrye.httpassured.internal.UriSupport;
import io.smallrye.httpassured.spi.HttpClientEngine;
import io.smallrye.httpassured.spi.WebSocketContext;
import io.smallrye.httpassured.toxic.Toxic;
import io.smallrye.httpassured.toxic.ToxicEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for establishing a WebSocket connection.
 * <pre>{@code
 * try (WsSession session = client.webSocket("/chat")
 *         .header("Authorization", "Bearer token")
 *         .queryParam("room", "general")
 *         .connect()) {
 *     session.sendText("hello");
 * }
 * }</pre>
 */
public final class WsSessionBuilder {

    private final HttpAssuredConfig config;
    private final HttpClientEngine engine;
    private final WebSocketContext.Builder contextBuilder;
    private final List<Toxic> toxics = new ArrayList<>();

    public WsSessionBuilder(HttpAssuredConfig config, HttpClientEngine engine, String path) {
        this.config = Objects.requireNonNull(config);
        this.engine = Objects.requireNonNull(engine);
        this.contextBuilder = WebSocketContext.builder().uri(resolveUri(config, path));
        // Apply default headers from config
        config.defaultHeaders().forEach(h -> contextBuilder.addHeader(h.name(), h.value()));
    }

    /**
     * Adds a header to the WebSocket handshake request.
     */
    public WsSessionBuilder header(String name, String value) {
        contextBuilder.addHeader(name, value);
        return this;
    }

    /**
     * Adds a query parameter to the WebSocket URI.
     */
    public WsSessionBuilder queryParam(String name, String value) {
        contextBuilder.queryParam(name, value);
        return this;
    }

    /**
     * Adds a toxic to the WebSocket connection.
     */
    public WsSessionBuilder toxic(Toxic toxic) {
        this.toxics.add(toxic);
        return this;
    }

    /**
     * Establishes the WebSocket connection and returns a session.
     * The returned session should be used with try-with-resources.
     *
     * @return an open WebSocket session
     */
    public WsSession connect() {
        WebSocketContext context = contextBuilder.build();
        HttpClientEngine engineToUse = engine;
        if (!toxics.isEmpty()) {
            engineToUse = new ToxicEngine(engineToUse, List.copyOf(toxics));
        }
        return engineToUse.openWebSocket(context)
                .await().indefinitely();
    }

    private String resolveUri(HttpAssuredConfig config, String path) {
        String base = config.baseUri();
        if (base != null && !base.isEmpty()) {
            // Convert http(s) to ws(s)
            String wsBase = base.replaceFirst("^http", "ws");
            // Inject port if configured
            if (config.port() > 0) {
                wsBase = UriSupport.injectPort(wsBase, config.port());
            }
            return wsBase + path;
        }
        return path;
    }
}
