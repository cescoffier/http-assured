package io.smallrye.httpassured.dsl;

import io.smallrye.httpassured.config.HttpAssuredConfig;
import io.smallrye.httpassured.http.ContentType;
import io.smallrye.httpassured.http.Cookie;
import io.smallrye.httpassured.http.Cookies;
import io.smallrye.httpassured.http.Headers;
import io.smallrye.httpassured.http.HttpMethod;
import io.smallrye.httpassured.http.HttpVersion;
import io.smallrye.httpassured.http.MultiPartSpec;
import io.smallrye.httpassured.log.RequestLogSpec;
import io.smallrye.httpassured.log.RequestLogger;
import io.smallrye.httpassured.spec.RequestSpec;
import io.smallrye.httpassured.spi.HttpClientEngine;
import io.smallrye.httpassured.spi.ObjectMapperProvider;
import io.smallrye.httpassured.spi.RawResponse;
import io.smallrye.httpassured.spi.RequestContext;
import io.smallrye.httpassured.spi.TrustOptions;
import io.smallrye.httpassured.toxic.Toxic;
import io.smallrye.httpassured.toxic.ToxicEngine;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.UrlStringRequestAdapter;
import oauth.signpost.exception.OAuthException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fluent builder for constructing and executing HTTP requests.
 * <p>
 * Corresponds to the "given" phase of the given/when/then DSL.
 * </p>
 * <pre>{@code
 * client.given()
 *     .header("Authorization", "Bearer token")
 *     .queryParam("page", "1")
 *     .contentType(ContentType.JSON)
 *     .body(myObject)
 * .when()
 *     .get("/users")
 * .then()
 *     .statusCode(200);
 * }</pre>
 */
public final class RequestBuilder {

    /**
     * Holds OAuth 1 credentials for deferred HMAC-SHA1 signing in {@link #execute}.
     */
    record OAuth1Credentials(String consumerKey, String consumerSecret,
                             String accessToken, String tokenSecret) {}

    private final HttpAssuredConfig config;
    private Headers headers;
    private final Map<String, List<String>> queryParams = new LinkedHashMap<>();
    private final Map<String, String> pathParams = new LinkedHashMap<>();
    private final Map<String, String> formParams = new LinkedHashMap<>();
    private ContentType contentType;
    private byte[] body;
    private boolean trustAll = false;
    private TrustOptions trustOptions;
    private final List<Toxic> toxics = new ArrayList<>();
    private final List<MultiPartSpec> multiParts = new ArrayList<>();
    private RequestLogSpec requestLogSpec;
    private OAuth1Credentials oauth1Credentials;
    private boolean followRedirects = true;
    private int maxRedirects = -1;
    private boolean urlEncodingEnabled = true;
    private HttpVersion httpVersion;

    public RequestBuilder(HttpAssuredConfig config) {
        this.config = config;
        // Start with default headers from config
        this.headers = config.defaultHeaders();
        // If the instance-level flag is set, pre-activate request logging on failure
        if (config.logRequestIfValidationFails()) {
            requestLogSpec = new RequestLogSpec(this, config.blacklistedHeaders());
            requestLogSpec.all();
        }
    }


    /**
     * Returns an {@link AuthBuilder} to configure request authentication.
     *
     * <p>Supported mechanisms:
     * <ul>
     *   <li>{@link AuthBuilder#basic(String, String)} — HTTP Basic auth</li>
     *   <li>{@link AuthBuilder#preemptive()}{@code .basic(user, pass)} — pre-emptive Basic auth</li>
     *   <li>{@link AuthBuilder#oauth2(String)} — OAuth 2 bearer token</li>
     *   <li>{@link AuthBuilder#oauth(String, String, String, String)} — OAuth 1 HMAC-SHA1</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * client.given()
     *     .auth().basic("user", "secret")
     *     .when().get("/secure");
     * }</pre>
     *
     * @return a new {@link AuthBuilder} backed by this request builder
     */
    public AuthBuilder auth() {
        return new AuthBuilder(this);
    }

    /** Called by {@link AuthBuilder} to store OAuth 1 credentials for deferred signing. */
    void setOAuth1Credentials(OAuth1Credentials credentials) {
        this.oauth1Credentials = credentials;
    }

    /** Called by {@link RedirectSpec} to store redirect follow preference. */
    void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /** Called by {@link RedirectSpec} to store max redirect count. */
    void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }


    /**
     * Returns a {@link RedirectSpec} to configure redirect behavior for this request.
     *
     * @return a new {@link RedirectSpec} backed by this request builder
     */
    public RedirectSpec redirects() {
        return new RedirectSpec(this);
    }


    /**
     * Adds a header to the request.
     */
    public RequestBuilder header(String name, String value) {
        this.headers = this.headers.with(name, value);
        return this;
    }

    /**
     * Adds multiple values for the same header name.
     *
     * @param name             the header name
     * @param firstValue       the first header value
     * @param additionalValues additional header values
     * @return this builder
     */
    public RequestBuilder header(String name, String firstValue, String... additionalValues) {
        this.headers = this.headers.with(name, firstValue);
        for (String value : additionalValues) {
            this.headers = this.headers.with(name, value);
        }
        return this;
    }

    /**
     * Adds a cookie to the request {@code Cookie} header.
     *
     * <p>Multiple cookies are combined into a single {@code Cookie} header as
     * {@code name1=value1; name2=value2} (RFC 6265 §4.2.1). If a {@code Cookie}
     * header already exists it is extended rather than replaced.
     *
     * @param name  cookie name
     * @param value cookie value
     * @return this builder
     */
    public RequestBuilder cookie(String name, String value) {
        String pair = name + "=" + value;
        var existing = this.headers.getValue("Cookie");
        String merged = existing.map(e -> e + "; " + pair).orElse(pair);
        this.headers = this.headers.replacing("Cookie", merged);
        return this;
    }

    /**
     * Adds a multi-value cookie to the request {@code Cookie} header.
     *
     * @param name         cookie name
     * @param firstValue   first cookie value
     * @param moreValues   additional cookie values
     * @return this builder
     */
    public RequestBuilder cookie(String name, String firstValue, String... moreValues) {
        cookie(name, firstValue);
        for (String v : moreValues) {
            cookie(name, v);
        }
        return this;
    }

    /**
     * Adds a cookie to the request {@code Cookie} header.
     *
     * @param cookie the cookie to send
     * @return this builder
     */
    public RequestBuilder cookie(Cookie cookie) {
        return cookie(cookie.name(), cookie.value());
    }

    /**
     * Adds all cookies from the given {@link Cookies} collection to the request
     * {@code Cookie} header.
     *
     * @param cookies the cookies to send
     * @return this builder
     */
    public RequestBuilder cookies(Cookies cookies) {
        for (Cookie c : cookies) {
            cookie(c);
        }
        return this;
    }

    /**
     * Sets the {@code Accept} header to the given MIME type string.
     *
     * @param contentType the MIME type to accept (e.g. "application/json")
     * @return this builder
     */
    public RequestBuilder accept(String contentType) {
        this.headers = this.headers.replacing("Accept", contentType);
        return this;
    }

    /**
     * Sets the {@code Accept} header using a {@link ContentType} enum value.
     *
     * @param contentType the content type to accept
     * @return this builder
     */
    public RequestBuilder accept(ContentType contentType) {
        return accept(contentType.value());
    }

    /**
     * Sets the Content-Type header.
     */
    public RequestBuilder contentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Adds a no-value query parameter (e.g. {@code ?key} without {@code =}).
     *
     * @param name the parameter name
     * @return this builder
     */
    public RequestBuilder queryParam(String name) {
        this.queryParams.computeIfAbsent(name, k -> new ArrayList<>()).add(null);
        return this;
    }

    /**
     * Adds a query parameter.
     * <p>
     * If the parameter already exists, the value is appended (multi-value support).
     * </p>
     */
    public RequestBuilder queryParam(String name, String value) {
        this.queryParams.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Adds a multi-value query parameter using varargs.
     *
     * @param name       the parameter name
     * @param firstValue the first value
     * @param moreValues additional values
     * @return this builder
     */
    public RequestBuilder queryParam(String name, String firstValue, String... moreValues) {
        List<String> list = this.queryParams.computeIfAbsent(name, k -> new ArrayList<>());
        list.add(firstValue);
        for (String v : moreValues) {
            list.add(v);
        }
        return this;
    }

    /**
     * Adds a multi-value query parameter from a list.
     *
     * @param name   the parameter name
     * @param values the parameter values
     * @return this builder
     */
    public RequestBuilder queryParam(String name, List<String> values) {
        this.queryParams.computeIfAbsent(name, k -> new ArrayList<>()).addAll(values);
        return this;
    }

    /**
     * Controls whether query parameter names and values are URL-encoded.
     * Enabled by default. Disable to send raw parameter values.
     *
     * @param enabled {@code false} to disable URL encoding
     * @return this builder
     */
    public RequestBuilder urlEncodingEnabled(boolean enabled) {
        this.urlEncodingEnabled = enabled;
        return this;
    }

    /**
     * Sets the HTTP protocol version for this request.
     *
     * @param version the HTTP version (HTTP_1_0, HTTP_1_1, HTTP_2, HTTP_3)
     * @return this builder
     */
    public RequestBuilder version(HttpVersion version) {
        this.httpVersion = version;
        return this;
    }

    /**
     * Adds a path parameter. Path parameters are substituted into the URI
     * template using {@code {name}} placeholders.
     */
    public RequestBuilder pathParam(String name, String value) {
        this.pathParams.put(name, value);
        return this;
    }


    /**
     * Adds a form parameter to be sent as {@code application/x-www-form-urlencoded} body.
     *
     * @param name  the parameter name
     * @param value the parameter value
     * @return this builder
     */
    public RequestBuilder formParam(String name, String value) {
        this.formParams.put(name, value);
        return this;
    }

    /**
     * Adds multiple form parameters from a map.
     *
     * @param params the parameters to add
     * @return this builder
     */
    public RequestBuilder formParams(Map<String, String> params) {
        this.formParams.putAll(params);
        return this;
    }

    /**
     * Adds multiple form parameters from name-value pairs.
     *
     * @param firstParamName  the first parameter name
     * @param firstParamValue the first parameter value
     * @param additionalPairs additional name-value pairs (must be even length)
     * @return this builder
     * @throws IllegalArgumentException if {@code additionalPairs} has an odd length
     */
    public RequestBuilder formParams(String firstParamName, String firstParamValue, String... additionalPairs) {
        if (additionalPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Form params must be provided as name-value pairs; got an odd number of additional arguments");
        }
        this.formParams.put(firstParamName, firstParamValue);
        for (int i = 0; i < additionalPairs.length; i += 2) {
            this.formParams.put(additionalPairs[i], additionalPairs[i + 1]);
        }
        return this;
    }


    /**
     * Sets the request body as a raw byte array.
     */
    public RequestBuilder body(byte[] body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the request body as a string (UTF-8 encoded).
     */
    public RequestBuilder body(String body) {
        this.body = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return this;
    }

    /**
     * Sets the request body by reading all bytes from the given input stream.
     * <p>
     * Unlike {@link #body(Object)}, this method does <em>not</em> auto-set the
     * Content-Type header.
     *
     * @param inputStream the input stream to read the body bytes from
     * @return this builder
     * @throws UncheckedIOException if reading from the stream fails
     */
    public RequestBuilder body(InputStream inputStream) {
        try {
            this.body = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /**
     * Sets the request body by serializing the given object using the configured
     * {@link ObjectMapperProvider}. Also sets Content-Type to JSON if not already set.
     */
    public RequestBuilder body(Object object) {
        if (object instanceof byte[] bytes) {
            this.body = bytes;
        } else if (object instanceof String str) {
            this.body = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } else {
            this.body = config.objectMapper().serialize(object);
            if (this.contentType == null) {
                this.contentType = ContentType.JSON;
            }
        }
        return this;
    }


    /**
     * Adds a file-upload part to the request using the given control name.
     *
     * @param controlName the form-field name
     * @param file        the file to upload
     * @return this builder
     */
    public RequestBuilder multiPart(String controlName, File file) {
        this.multiParts.add(MultiPartSpec.file(controlName, file));
        return this;
    }

    /**
     * Adds a file-upload part to the request using the default control name {@code "file"}.
     *
     * @param file the file to upload
     * @return this builder
     */
    public RequestBuilder multiPart(File file) {
        this.multiParts.add(MultiPartSpec.file(file));
        return this;
    }

    /**
     * Adds a binary-upload part to the request.
     *
     * @param controlName the form-field name
     * @param fileName    the file name advertised in the part header
     * @param content     the raw bytes
     * @param mimeType    the MIME type
     * @return this builder
     */
    public RequestBuilder multiPart(String controlName, String fileName, byte[] content, String mimeType) {
        this.multiParts.add(MultiPartSpec.bytes(controlName, fileName, content, mimeType));
        return this;
    }

    /**
     * Adds a plain-text field to a multipart request.
     *
     * @param controlName the form-field name
     * @param content     the text value
     * @return this builder
     */
    public RequestBuilder multiPart(String controlName, String content) {
        this.multiParts.add(MultiPartSpec.text(controlName, content));
        return this;
    }

    /**
     * Applies a reusable request specification.
     */
    public RequestBuilder spec(RequestSpec spec) {
        if (spec.headers() != null) {
            this.headers = this.headers.merge(spec.headers());
        }
        spec.queryParams().forEach((name, values) ->
                this.queryParams.computeIfAbsent(name, k -> new ArrayList<>()).addAll(values));
        this.pathParams.putAll(spec.pathParams());
        this.formParams.putAll(spec.formParams());
        if (spec.contentType() != null) {
            this.contentType = spec.contentType();
        }
        if (spec.body() != null) {
            this.body = spec.body();
        }
        return this;
    }

    /**
     * Controls whether the TLS certificate of the target server should be trusted without validation.
     */
    public RequestBuilder trustAll(boolean trustAll) {
        this.trustAll = trustAll;
        return this;
    }

    /**
     * Configures a specific trust store for TLS certificate validation.
     *
     * @param trustOptions the trust store to use (PEM CA cert, JKS, or PKCS12)
     */
    public RequestBuilder trustOptions(TrustOptions trustOptions) {
        this.trustOptions = trustOptions;
        return this;
    }

    /**
     * Adds a fault injection toxic to this request.
     */
    public RequestBuilder toxic(Toxic toxic) {
        this.toxics.add(toxic);
        return this;
    }

    /**
     * Returns a {@link RequestLogSpec} to configure what to log about this request.
     */
    public RequestLogSpec log() {
        if (requestLogSpec == null) {
            requestLogSpec = new RequestLogSpec(this, config.blacklistedHeaders());
        }
        return requestLogSpec;
    }


    /**
     * Transitions to the "when" phase. Syntactic sugar — returns this builder.
     */
    public RequestBuilder when() {
        return this;
    }


    public Response get(String path, Object... pathParamValues) {
        return execute(HttpMethod.GET, path, pathParamValues);
    }

    public Response post(String path, Object... pathParamValues) {
        return execute(HttpMethod.POST, path, pathParamValues);
    }

    public Response put(String path, Object... pathParamValues) {
        return execute(HttpMethod.PUT, path, pathParamValues);
    }

    public Response delete(String path, Object... pathParamValues) {
        return execute(HttpMethod.DELETE, path, pathParamValues);
    }

    public Response patch(String path, Object... pathParamValues) {
        return execute(HttpMethod.PATCH, path, pathParamValues);
    }

    public Response head(String path, Object... pathParamValues) {
        return execute(HttpMethod.HEAD, path, pathParamValues);
    }

    public Response options(String path, Object... pathParamValues) {
        return execute(HttpMethod.OPTIONS, path, pathParamValues);
    }

    /**
     * Executes a request with the given HTTP method and path.
     */
    public Response request(HttpMethod method, String path, Object... pathParamValues) {
        return execute(method, path, pathParamValues);
    }

    /**
     * Executes a request with the given HTTP method string and path.
     */
    public Response request(String method, String path, Object... pathParamValues) {
        return execute(HttpMethod.valueOf(method.toUpperCase()), path, pathParamValues);
    }

    private Response execute(HttpMethod method, String path, Object[] pathParamValues) {
        if (body != null && !multiParts.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot combine body() and multiPart() on the same request");
        }

        String resolvedPath = substitutePathParams(path, pathParamValues);
        String fullUri = config.resolveUri(resolvedPath);

        RequestContext.Builder ctxBuilder = RequestContext.builder()
                .method(method)
                .uri(fullUri)
                .headers(headers)
                .trustAll(trustAll)
                .trustOptions(trustOptions);

        ctxBuilder.queryParams(queryParams);
        pathParams.forEach(ctxBuilder::pathParam);

        // Form params: encode as URL-encoded body
        if (!formParams.isEmpty()) {
            if (body != null) {
                throw new IllegalStateException("Cannot use both body() and formParam() in the same request");
            }
            String encoded = formParams.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                            + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            ctxBuilder.body(encoded.getBytes(StandardCharsets.UTF_8));
            if (contentType == null) {
                contentType = ContentType.FORM_URL_ENCODED;
            }
        }

        if (body != null) {
            ctxBuilder.body(body);
        }

        // Auto-set content type for multipart if not explicitly set
        if (!multiParts.isEmpty() && contentType == null) {
            contentType = ContentType.MULTIPART_FORM_DATA;
        }

        if (contentType != null) {
            ctxBuilder.contentType(contentType);
            // Do not set Content-Type header manually for multipart — Vert.x sets it with boundary
            if (multiParts.isEmpty()) {
                ctxBuilder.addHeader("Content-Type", contentType.value());
            }
        }

        // Store multipart parts as an attribute for the engine to pick up
        if (!multiParts.isEmpty()) {
            ctxBuilder.attribute("multiParts", List.copyOf(multiParts));
        }

        // Store redirect config as attributes for the engine
        if (!followRedirects) {
            ctxBuilder.attribute("followRedirects", false);
        }
        if (maxRedirects > 0) {
            ctxBuilder.attribute("maxRedirects", maxRedirects);
        }
        if (!urlEncodingEnabled) {
            ctxBuilder.attribute("urlEncodingEnabled", false);
        }
        if (httpVersion != null) {
            ctxBuilder.attribute("httpVersion", httpVersion);
        }

        // OAuth 1: sign with the fully-assembled URI (base + path + query params already on ctxBuilder)
        if (oauth1Credentials != null) {
            String signedHeader = buildOAuth1Header(oauth1Credentials, fullUri);
            ctxBuilder.addHeader("Authorization", signedHeader);
        }

        RequestContext requestContext = ctxBuilder.build();

        if (requestLogSpec != null && requestLogSpec.hasFields()) {
            RequestLogger.log(requestContext, requestLogSpec.fields(), requestLogSpec.blacklist());
        }

        HttpClientEngine engine = config.engine();
        if (!toxics.isEmpty()) {
            engine = new ToxicEngine(engine, List.copyOf(toxics));
        }
        RawResponse rawResponse = engine.execute(requestContext);

        return new Response(rawResponse, config);
    }

    private static String buildOAuth1Header(OAuth1Credentials creds, String uri) {
        try {
            DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(
                    creds.consumerKey(), creds.consumerSecret());
            consumer.setTokenWithSecret(creds.accessToken(), creds.tokenSecret());
            CapturingRequestAdapter adapter = new CapturingRequestAdapter(uri);
            consumer.sign(adapter);
            String header = adapter.getCapturedAuthorizationHeader();
            if (header == null) {
                throw new IllegalStateException("OAuth 1 signing produced no Authorization header");
            }
            return header;
        } catch (OAuthException e) {
            throw new IllegalStateException("OAuth 1 signing failed: " + e.getMessage(), e);
        }
    }

    private static final class CapturingRequestAdapter extends UrlStringRequestAdapter {
        private String authorizationHeader;

        CapturingRequestAdapter(String url) {
            super(url);
        }

        @Override
        public void setHeader(String name, String value) {
            if ("Authorization".equalsIgnoreCase(name)) {
                this.authorizationHeader = value;
            }
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return authorizationHeader;
            }
            return null;
        }

        String getCapturedAuthorizationHeader() {
            return authorizationHeader;
        }
    }

    private String substitutePathParams(String path, Object[] values) {
        if (values == null || values.length == 0) {
            String result = path;
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return result;
        }

        String result = path;
        int i = 0;
        while (result.contains("{") && i < values.length) {
            int start = result.indexOf('{');
            int end = result.indexOf('}', start);
            if (end == -1) break;
            result = result.substring(0, start) + values[i] + result.substring(end + 1);
            i++;
        }
        return result;
    }
}
