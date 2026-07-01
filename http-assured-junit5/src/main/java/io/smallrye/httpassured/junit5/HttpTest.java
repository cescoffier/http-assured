package io.smallrye.httpassured.junit5;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class for http-assured integration.
 * <p>
 * Registers the {@link HttpAssuredExtension} which provides:
 * <ul>
 *   <li>Automatic {@link io.smallrye.httpassured.HttpAssured} client lifecycle management</li>
 *   <li>Parameter injection of {@code HttpAssured} into test methods</li>
 *   <li>Parameter injection of {@link io.smallrye.httpassured.websocket.WsSession} via {@link WebSocket}</li>
 *   <li>Support for {@link WithToxic} annotations</li>
 *   <li>Automatic request/response dump on test failure</li>
 * </ul>
 * </p>
 * <pre>{@code
 * @HttpTest(baseUri = "http://localhost:8080")
 * class UserApiTest {
 *
 *     @Test
 *     void shouldGetUser(HttpAssured client) {
 *         client.given()
 *             .when().get("/users/1")
 *             .then().statusCode(200)
 *             .body("name", isEqualTo("John"));
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(HttpAssuredExtension.class)
public @interface HttpTest {

    /**
     * Base URI for all requests. Defaults to empty (must be configured).
     */
    String baseUri() default "";

    /**
     * Port for all requests. Defaults to -1 (use URI port).
     */
    int port() default -1;

    /**
     * Base path appended after the base URI.
     */
    String basePath() default "";
}
