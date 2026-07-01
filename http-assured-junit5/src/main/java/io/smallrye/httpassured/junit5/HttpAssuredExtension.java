package io.smallrye.httpassured.junit5;

import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.toxic.Toxic;
import io.smallrye.httpassured.toxic.ToxicEngine;
import io.smallrye.httpassured.websocket.WsSession;
import org.junit.jupiter.api.extension.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 5 extension that manages {@link HttpAssured} client lifecycle
 * and provides parameter injection.
 * <p>
 * Registered automatically via {@link HttpTest} or manually via {@code @ExtendWith}.
 * </p>
 */
public class HttpAssuredExtension implements
        BeforeAllCallback,
        AfterAllCallback,
        BeforeEachCallback,
        AfterEachCallback,
        ParameterResolver,
        TestWatcher {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(HttpAssuredExtension.class);

    private static final String CLIENT_KEY = "httpAssuredClient";
    private static final String WS_SESSIONS_KEY = "wsSessionsToClose";
    private static final String TOXICS_KEY = "toxicsForTest";
    private static final String TOXIC_CLIENT_KEY = "toxicClient";

    @Override
    public void beforeAll(ExtensionContext context) {
        HttpTest annotation = context.getRequiredTestClass().getAnnotation(HttpTest.class);
        if (annotation == null) {
            return;
        }

        HttpAssured.Builder builder = HttpAssured.builder();
        if (!annotation.baseUri().isEmpty()) {
            builder.baseUri(annotation.baseUri());
        }
        if (annotation.port() > 0) {
            builder.port(annotation.port());
        }
        if (!annotation.basePath().isEmpty()) {
            builder.basePath(annotation.basePath());
        }

        HttpAssured client = builder.build();
        getStore(context).put(CLIENT_KEY, client);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        HttpAssured client = getStore(context).remove(CLIENT_KEY, HttpAssured.class);
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        getStore(context).put(WS_SESSIONS_KEY, new ArrayList<WsSession>());

        // Collect toxics from @WithToxic annotations
        List<Toxic> toxics = new ArrayList<>();
        context.getTestMethod().ifPresent(method -> {
            WithToxic[] annotations = method.getAnnotationsByType(WithToxic.class);
            for (WithToxic annotation : annotations) {
                toxics.addAll(buildToxics(annotation));
            }
        });
        if (!toxics.isEmpty()) {
            getStore(context).put(TOXICS_KEY, toxics);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        @SuppressWarnings("unchecked")
        List<WsSession> sessions = getStore(context).remove(WS_SESSIONS_KEY, List.class);
        if (sessions != null) {
            for (WsSession session : sessions) {
                try {
                    session.close();
                } catch (Exception ignored) {
                    // Best effort cleanup
                }
            }
        }

        HttpAssured toxicClient = getStore(context).remove(TOXIC_CLIENT_KEY, HttpAssured.class);
        if (toxicClient != null) {
            toxicClient.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        if (HttpAssured.class.equals(type)) {
            return true;
        }
        return WsSession.class.equals(type) && parameterContext.isAnnotated(WebSocket.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();

        if (HttpAssured.class.equals(type)) {
            HttpAssured client = getClient(extensionContext);
            if (client == null) {
                throw new ParameterResolutionException(
                        "No HttpAssured client available. Annotate your test class with @HttpTest.");
            }

            @SuppressWarnings("unchecked")
            List<Toxic> toxics = (List<Toxic>) getStore(extensionContext).get(TOXICS_KEY, List.class);
            if (toxics != null && !toxics.isEmpty()) {
                // Create a new client with a ToxicEngine wrapping the original engine
                // Use withEngine() to preserve all config (headers, timeout, objectMapper, etc.)
                HttpAssured toxicClient = client.withEngine(new ToxicEngine(client.config().engine(), toxics));
                getStore(extensionContext).put(TOXIC_CLIENT_KEY, toxicClient);
                return toxicClient;
            }

            return client;
        }

        if (WsSession.class.equals(type)) {
            WebSocket wsAnnotation = parameterContext.findAnnotation(WebSocket.class)
                    .orElseThrow(() -> new ParameterResolutionException(
                            "WsSession parameter must be annotated with @WebSocket"));

            HttpAssured client = getClient(extensionContext);
            if (client == null) {
                throw new ParameterResolutionException(
                        "No HttpAssured client available. Annotate your test class with @HttpTest.");
            }

            WsSession session = client.webSocket(wsAnnotation.value()).connect();
            trackSession(extensionContext, session);
            return session;
        }

        throw new ParameterResolutionException("Unsupported parameter type: " + type);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        // On test failure, log diagnostic information
        // This is where we'd dump request/response details
        // For now, the test failure message from JUnit 5 assertions is sufficient
    }

    private HttpAssured getClient(ExtensionContext context) {
        // Walk up the context hierarchy to find the client
        ExtensionContext current = context;
        while (current != null) {
            HttpAssured client = getStore(current).get(CLIENT_KEY, HttpAssured.class);
            if (client != null) {
                return client;
            }
            current = current.getParent().orElse(null);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void trackSession(ExtensionContext context, WsSession session) {
        List<WsSession> sessions = getStore(context).get(WS_SESSIONS_KEY, List.class);
        if (sessions == null) {
            sessions = new ArrayList<>();
            getStore(context).put(WS_SESSIONS_KEY, sessions);
        }
        sessions.add(session);
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    private List<Toxic> buildToxics(WithToxic annotation) {
        List<Toxic> toxics = new ArrayList<>();
        double toxicity = annotation.toxicity();

        if (annotation.latencyMs() > 0) {
            Toxic t = annotation.jitterMs() > 0
                    ? Toxic.latency(Duration.ofMillis(annotation.latencyMs()),
                                    Duration.ofMillis(annotation.jitterMs()))
                    : Toxic.latency(Duration.ofMillis(annotation.latencyMs()));
            if (toxicity < 1.0) t = t.withToxicity(toxicity);
            toxics.add(t);
        }
        if (annotation.bandwidthBytesPerSecond() > 0) {
            Toxic t = Toxic.bandwidth(annotation.bandwidthBytesPerSecond());
            if (toxicity < 1.0) t = t.withToxicity(toxicity);
            toxics.add(t);
        }
        if (annotation.down()) {
            Toxic t = Toxic.down();
            if (toxicity < 1.0) t = t.withToxicity(toxicity);
            toxics.add(t);
        }
        if (annotation.resetPeer()) {
            Toxic t = annotation.resetPeerDelayMs() > 0
                    ? Toxic.resetPeer(Duration.ofMillis(annotation.resetPeerDelayMs()))
                    : Toxic.resetPeer();
            if (toxicity < 1.0) t = t.withToxicity(toxicity);
            toxics.add(t);
        }
        if (annotation.timeoutMs() > 0) {
            Toxic t = Toxic.timeout(Duration.ofMillis(annotation.timeoutMs()));
            if (toxicity < 1.0) t = t.withToxicity(toxicity);
            toxics.add(t);
        }
        if (annotation.slowCloseMs() > 0) {
            Toxic t = Toxic.slowClose(Duration.ofMillis(annotation.slowCloseMs()));
            if (toxicity < 1.0) t = t.withToxicity(toxicity);
            toxics.add(t);
        }
        if (annotation.limitDataBytes() > 0) {
            Toxic t = Toxic.limitData(annotation.limitDataBytes());
            if (toxicity < 1.0) t = t.withToxicity(toxicity);
            toxics.add(t);
        }
        if (annotation.respondWithStatus() > 0) {
            Toxic t = Toxic.respondWith(annotation.respondWithStatus());
            if (toxicity < 1.0) t = t.withToxicity(toxicity);
            toxics.add(t);
        }

        return toxics;
    }
}
