package io.smallrye.httpassured.engine.vertx;

import io.smallrye.httpassured.HttpAssuredException;
import io.smallrye.httpassured.websocket.WsSession;
import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Vert.x-backed WebSocket session implementation.
 */
class VertxWsSession implements WsSession {

    private final WebSocket webSocket;
    private final BlockingQueue<Object> textQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> binaryQueue = new LinkedBlockingQueue<>();
    private volatile boolean closed = false;

    // Sentinel value for connection close
    private static final String CLOSE_SENTINEL = "\0__CLOSED__\0";

    VertxWsSession(WebSocket webSocket) {
        this.webSocket = webSocket;

        webSocket.textMessageHandler(message -> textQueue.offer(message));

        webSocket.binaryMessageHandler(buffer -> binaryQueue.offer(buffer.getBytes()));

        webSocket.closeHandler(v -> {
            closed = true;
            textQueue.offer(CLOSE_SENTINEL);
            binaryQueue.offer(new byte[0]);
        });

        webSocket.exceptionHandler(err -> {
            closed = true;
            textQueue.offer(CLOSE_SENTINEL);
        });
    }

    @Override
    public WsSession sendText(String message) {
        webSocket.writeTextMessage(message);
        return this;
    }

    @Override
    public WsSession sendBinary(byte[] data) {
        webSocket.writeBinaryMessage(Buffer.buffer(data));
        return this;
    }

    @Override
    public WsSession sendPing() {
        webSocket.writePing(Buffer.buffer());
        return this;
    }

    @Override
    public String awaitText(Duration timeout) {
        try {
            Object message = textQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (message == null) {
                throw new HttpAssuredException(
                        "Timed out waiting for WebSocket text message after " + timeout);
            }
            if (CLOSE_SENTINEL.equals(message)) {
                throw new HttpAssuredException("WebSocket connection closed while waiting for message");
            }
            return (String) message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpAssuredException("Interrupted while waiting for WebSocket message", e);
        }
    }

    @Override
    public byte[] awaitBinary(Duration timeout) {
        try {
            byte[] data = binaryQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (data == null) {
                throw new HttpAssuredException(
                        "Timed out waiting for WebSocket binary message after " + timeout);
            }
            if (data.length == 0 && closed) {
                throw new HttpAssuredException("WebSocket connection closed while waiting for message");
            }
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpAssuredException("Interrupted while waiting for WebSocket message", e);
        }
    }

    @Override
    public Multi<String> textMessages() {
        return Multi.createFrom().emitter(emitter -> {
            // Switch to live handler first so no new messages are lost
            webSocket.textMessageHandler(msg -> emitter.emit(msg));
            webSocket.closeHandler(v -> emitter.complete());
            webSocket.exceptionHandler(emitter::fail);

            // Drain messages that were buffered before subscription
            Object queued;
            while ((queued = textQueue.poll()) != null) {
                if (CLOSE_SENTINEL.equals(queued)) {
                    emitter.complete();
                    return;
                }
                emitter.emit((String) queued);
            }
            if (closed) {
                emitter.complete();
            }
        });
    }

    @Override
    public Multi<byte[]> binaryMessages() {
        return Multi.createFrom().emitter(emitter -> {
            webSocket.binaryMessageHandler(buffer -> emitter.emit(buffer.getBytes()));
            webSocket.closeHandler(v -> emitter.complete());
            webSocket.exceptionHandler(emitter::fail);

            byte[] queued;
            while ((queued = binaryQueue.poll()) != null) {
                if (queued.length == 0 && closed) {
                    emitter.complete();
                    return;
                }
                emitter.emit(queued);
            }
            if (closed) {
                emitter.complete();
            }
        });
    }

    @Override
    public boolean isOpen() {
        return !closed && !webSocket.isClosed();
    }

    @Override
    public void close() {
        if (!webSocket.isClosed()) {
            webSocket.close();
        }
        closed = true;
    }

    @Override
    public void close(int statusCode, String reason) {
        if (!webSocket.isClosed()) {
            webSocket.close((short) statusCode, reason);
        }
        closed = true;
    }
}
