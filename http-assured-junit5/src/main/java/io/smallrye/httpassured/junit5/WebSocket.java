package io.smallrye.httpassured.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link io.smallrye.httpassured.websocket.WsSession} into a test method parameter.
 * <p>
 * The session is automatically connected before the test and closed after.
 * </p>
 * <pre>{@code
 * @Test
 * void shouldChat(@WebSocket("/chat") WsSession session) {
 *     session.sendText("hello");
 *     assertEquals("world", session.awaitText(Duration.ofSeconds(5)));
 * }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocket {

    /**
     * The WebSocket endpoint path.
     */
    String value();
}
