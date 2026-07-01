package io.smallrye.httpassured.engine.vertx;

import io.smallrye.httpassured.HttpAssuredException;
import io.smallrye.httpassured.http.Header;
import io.smallrye.httpassured.http.Headers;
import io.smallrye.httpassured.http.HttpMethod;
import io.smallrye.httpassured.http.HttpVersion;
import io.smallrye.httpassured.http.MultiPartSpec;
import io.smallrye.httpassured.spi.HttpClientEngine;
import io.smallrye.httpassured.spi.RawResponse;
import io.smallrye.httpassured.spi.RequestContext;
import io.smallrye.httpassured.spi.TrustOptions;
import io.smallrye.httpassured.spi.WebSocketContext;
import io.smallrye.httpassured.websocket.WsSession;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientMultipartForm;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.PoolOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.PemTrustOptions;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP client engine backed by Vert.x HTTP Client.
 * <p>
 * Supports HTTP/1.1, HTTP/2, and HTTP/3. WebSocket support uses the Vert.x core WebSocket client.
 * </p>
 */
public class VertxHttpEngine implements HttpClientEngine {

    private final Vertx vertx;
    private final HttpClient httpClient;
    private final WebSocketClient webSocketClient;
    private final boolean ownsVertx;
    private final long timeoutMs;

    public VertxHttpEngine() {
        this(Vertx.vertx(), true, 30_000);
    }

    public VertxHttpEngine(Vertx vertx) {
        this(vertx, false, 30_000);
    }

    public VertxHttpEngine(Vertx vertx, boolean ownsVertx, long timeoutMs) {
        this(vertx, ownsVertx, timeoutMs,
                new HttpClientOptions()
                        .setConnectTimeout((int) timeoutMs)
                        .setMaxRedirects(5));
    }

    public VertxHttpEngine(Vertx vertx, boolean ownsVertx, long timeoutMs, HttpClientOptions clientOptions) {
        this.vertx = vertx;
        this.ownsVertx = ownsVertx;
        this.timeoutMs = timeoutMs;
        this.httpClient = vertx.createHttpClient(clientOptions, new PoolOptions().setHttp1MaxSize(50).setHttp2MaxSize(50));
        this.webSocketClient = vertx.createWebSocketClient();
    }

    public VertxHttpEngine(Vertx vertx, boolean ownsVertx, long timeoutMs, HttpClientConfig clientConfig) {
        this.vertx = vertx;
        this.ownsVertx = ownsVertx;
        this.timeoutMs = timeoutMs;
        this.httpClient = vertx.httpClientBuilder().with(clientConfig).build();
        this.webSocketClient = vertx.createWebSocketClient();
    }

    @Override
    public RawResponse execute(RequestContext request) {
        return executeAsync(request)
                .await().atMost(java.time.Duration.ofMillis(timeoutMs));
    }

    @Override
    public Uni<RawResponse> executeAsync(RequestContext request) {
        return Uni.createFrom().emitter(emitter -> {
            long startTime = System.nanoTime();
            try {
                URI uri = URI.create(request.uri());
                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getRawPath();
                if (path == null || path.isEmpty()) {
                    path = "/";
                }
                boolean urlEncode = (boolean) request.attributes()
                        .getOrDefault("urlEncodingEnabled", true);
                path = appendQueryParams(path, uri.getRawQuery(), request.queryParams(), urlEncode);
                boolean ssl = "https".equalsIgnoreCase(uri.getScheme());

                if (port == -1) {
                    port = ssl ? 443 : 80;
                }

                int maxRedirects = (int) request.attributes().getOrDefault("maxRedirects", -1);
                HttpVersion httpVersion = (HttpVersion) request.attributes().get("httpVersion");
                @SuppressWarnings("unchecked")
                List<MultiPartSpec> multiParts = (List<MultiPartSpec>) request.attributes().get("multiParts");

                RequestOptions options = buildRequestOptions(request, uri, ssl, host, port);
                ClientSelection clientSelection = selectClient(httpVersion, maxRedirects, options);

                sendRequest(clientSelection, options, path, multiParts, request, startTime, emitter);
            } catch (Exception e) {
                emitter.fail(new HttpAssuredException("Failed to build request", e));
            }
        });
    }

    private RequestOptions buildRequestOptions(RequestContext request, URI uri, boolean ssl, String host, int port) {
        RequestOptions options = new RequestOptions();
        options.setMethod(toVertxMethod(request.method()));
        options.setHost(host);
        options.setPort(port);

        if (ssl) {
            options.setSsl(true);
            ClientSSLOptions sslOptions = new ClientSSLOptions().setTrustAll(request.trustAll());
            if (!request.trustAll()) {
                request.trustOptions().ifPresent(trustOpts -> applyTrustOptions(sslOptions, trustOpts));
            }
            options.setSslOptions(sslOptions);
        }

        for (Header header : request.headers()) {
            options.addHeader(header.name(), header.value());
        }

        boolean followRedirects = (boolean) request.attributes()
                .getOrDefault("followRedirects", true);
        options.setFollowRedirects(followRedirects);
        options.setTimeout(timeoutMs);

        return options;
    }

    private ClientSelection selectClient(HttpVersion httpVersion, int maxRedirects, RequestOptions options) {
        HttpClient clientToUse;
        boolean closeAfter = false;

        if (httpVersion == HttpVersion.HTTP_3) {
            HttpClientConfig h3Config = new HttpClientConfig()
                    .setVersions(io.vertx.core.http.HttpVersion.HTTP_3)
                    .setConnectTimeout(java.time.Duration.ofMillis(timeoutMs));
            if (maxRedirects > 0) {
                h3Config.setMaxRedirects(maxRedirects);
            }
            clientToUse = vertx.httpClientBuilder().with(h3Config).build();
            closeAfter = true;
        } else if (httpVersion == HttpVersion.HTTP_2) {
            HttpClientOptions perRequestOptions = new HttpClientOptions()
                    .setProtocolVersion(io.vertx.core.http.HttpVersion.HTTP_2)
                    .setUseAlpn(true)
                    .setConnectTimeout((int) timeoutMs);
            if (maxRedirects > 0) {
                perRequestOptions.setMaxRedirects(maxRedirects);
            }
            clientToUse = vertx.createHttpClient(perRequestOptions,
                    new PoolOptions().setHttp1MaxSize(5).setHttp2MaxSize(5));
            closeAfter = true;
            options.setProtocolVersion(io.vertx.core.http.HttpVersion.HTTP_2);
        } else if (maxRedirects > 0) {
            HttpClientOptions perRequestOptions = new HttpClientOptions()
                    .setConnectTimeout((int) timeoutMs)
                    .setMaxRedirects(maxRedirects);
            clientToUse = vertx.createHttpClient(perRequestOptions,
                    new PoolOptions().setHttp1MaxSize(5));
            closeAfter = true;
        } else {
            clientToUse = httpClient;
        }

        if (httpVersion == HttpVersion.HTTP_1_0) {
            options.setProtocolVersion(io.vertx.core.http.HttpVersion.HTTP_1_0);
        } else if (httpVersion == HttpVersion.HTTP_1_1) {
            options.setProtocolVersion(io.vertx.core.http.HttpVersion.HTTP_1_1);
        }

        return new ClientSelection(clientToUse, closeAfter);
    }

    private ClientMultipartForm buildMultipartForm(List<MultiPartSpec> multiParts) {
        ClientMultipartForm form = ClientMultipartForm.multipartForm();
        for (MultiPartSpec part : multiParts) {
            if (part.isTextField()) {
                form.attribute(part.controlName(),
                        new String(part.content(), StandardCharsets.UTF_8));
            } else {
                form.binaryFileUpload(part.controlName(),
                        part.fileName(),
                        part.mimeType(),
                        Buffer.buffer(part.content()));
            }
        }
        return form;
    }

    private void sendRequest(ClientSelection clientSelection, RequestOptions options, String path,
                            List<MultiPartSpec> multiParts, RequestContext request,
                            long startTime, io.smallrye.mutiny.subscription.UniEmitter<? super RawResponse> emitter) {
        clientSelection.client().request(options)
                .map(req -> {
                    req.setURI(path);
                    if (multiParts == null && request.body().isPresent()) {
                        req.setChunked(true);
                    }
                    return req;
                })
                .flatMap(req -> {
                    if (multiParts != null && !multiParts.isEmpty()) {
                        return req.send(buildMultipartForm(multiParts));
                    } else if (request.body().isPresent()) {
                        return req.send(Buffer.buffer(request.body().get()));
                    } else {
                        return req.send();
                    }
                })
                .flatMap(resp -> resp.body().map(b -> Tuple2.of(resp, b)))
                .onComplete(ar -> {
                    long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
                    if (clientSelection.closeAfter()) {
                        clientSelection.client().close();
                    }
                    if (ar.succeeded()) {
                        emitter.complete(toRawResponse(ar.result(), elapsedMs));
                    } else {
                        emitter.fail(new HttpAssuredException("Request failed", ar.cause()));
                    }
                });
    }

    private record ClientSelection(HttpClient client, boolean closeAfter) {}

    @Override
    public Uni<WsSession> openWebSocket(WebSocketContext context) {
        return Uni.createFrom().emitter(emitter -> {
            try {
                URI uri = URI.create(context.uri());
                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getRawPath();
                if (path == null || path.isEmpty()) {
                    path = "/";
                }
                path = appendSingleValueQueryParams(path, uri.getRawQuery(), context.queryParams());
                boolean ssl = "wss".equalsIgnoreCase(uri.getScheme());

                if (port == -1) {
                    port = ssl ? 443 : 80;
                }

                WebSocketConnectOptions connectOptions = new WebSocketConnectOptions()
                        .setHost(host)
                        .setPort(port)
                        .setURI(path)
                        .setSsl(ssl);

                for (Header header : context.headers()) {
                    connectOptions.addHeader(header.name(), header.value());
                }

                webSocketClient.connect(connectOptions)
                        .onComplete(ar -> {
                            if (ar.succeeded()) {
                                emitter.complete(new VertxWsSession(ar.result()));
                            } else {
                                emitter.fail(new HttpAssuredException("WebSocket connection failed", ar.cause()));
                            }
                        });
            } catch (Exception e) {
                emitter.fail(new HttpAssuredException("Failed to open WebSocket", e));
            }
        });
    }

    @Override
    public void close() {
        httpClient.close();
        webSocketClient.close();
        if (ownsVertx) {
            vertx.close();
        }
    }

    private static void applyTrustOptions(ClientSSLOptions sslOptions, TrustOptions trustOpts) {
        if (trustOpts instanceof TrustOptions.Pem pem) {
            sslOptions.setTrustOptions(new PemTrustOptions().addCertPath(pem.certPath().toString()));
        } else if (trustOpts instanceof TrustOptions.Jks jks) {
            sslOptions.setTrustOptions(new KeyStoreOptions().setType("JKS")
                    .setPath(jks.keyStorePath().toString()).setPassword(jks.password()));
        } else if (trustOpts instanceof TrustOptions.Pkcs12 p12) {
            sslOptions.setTrustOptions(new KeyStoreOptions().setType("PKCS12")
                    .setPath(p12.keyStorePath().toString()).setPassword(p12.password()));
        }
    }

    private RawResponse toRawResponse(Tuple2<HttpClientResponse, Buffer> response, long responseTimeMs) {
        List<Header> headerList = new ArrayList<>();
        response.getItem1().headers().forEach(entry ->
                headerList.add(new Header(entry.getKey(), entry.getValue())));

        byte[] body = response.getItem2() != null ? response.getItem2().getBytes() : new byte[0];

        return new RawResponse(
                response.getItem1().statusCode(),
                response.getItem1().statusMessage(),
                new Headers(headerList),
                body,
                responseTimeMs,
                fromVertxVersion(response.getItem1().version())
        );
    }

    private static HttpVersion fromVertxVersion(io.vertx.core.http.HttpVersion version) {
        return switch (version) {
            case HTTP_1_0 -> HttpVersion.HTTP_1_0;
            case HTTP_1_1 -> HttpVersion.HTTP_1_1;
            case HTTP_2 -> HttpVersion.HTTP_2;
            case HTTP_3 -> HttpVersion.HTTP_3;
        };
    }

    private static String appendQueryParams(String path, String existingQuery,
                                               Map<String, List<String>> queryParams, boolean urlEncode) {
        StringBuilder sb = new StringBuilder(path);
        boolean hasQuery = existingQuery != null && !existingQuery.isEmpty();
        if (hasQuery) {
            sb.append('?').append(existingQuery);
        }
        for (Map.Entry<String, List<String>> param : queryParams.entrySet()) {
            String key = urlEncode ? URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8) : param.getKey();
            for (String value : param.getValue()) {
                sb.append(hasQuery ? '&' : '?');
                hasQuery = true;
                if (value == null) {
                    // No-value param: emit ?key without =
                    sb.append(key);
                } else {
                    sb.append(key)
                            .append('=')
                            .append(urlEncode ? URLEncoder.encode(value, StandardCharsets.UTF_8) : value);
                }
            }
        }
        return sb.toString();
    }

    /**
     * WebSocket context still uses single-value query params.
     */
    private static String appendSingleValueQueryParams(String path, String existingQuery, Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder(path);
        boolean hasQuery = existingQuery != null && !existingQuery.isEmpty();
        if (hasQuery) {
            sb.append('?').append(existingQuery);
        }
        for (Map.Entry<String, String> param : queryParams.entrySet()) {
            sb.append(hasQuery ? '&' : '?');
            hasQuery = true;
            sb.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private io.vertx.core.http.HttpMethod toVertxMethod(HttpMethod method) {
        return switch (method) {
            case GET -> io.vertx.core.http.HttpMethod.GET;
            case POST -> io.vertx.core.http.HttpMethod.POST;
            case PUT -> io.vertx.core.http.HttpMethod.PUT;
            case DELETE -> io.vertx.core.http.HttpMethod.DELETE;
            case PATCH -> io.vertx.core.http.HttpMethod.PATCH;
            case HEAD -> io.vertx.core.http.HttpMethod.HEAD;
            case OPTIONS -> io.vertx.core.http.HttpMethod.OPTIONS;
        };
    }
}
