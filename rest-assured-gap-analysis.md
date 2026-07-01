# REST Assured vs http-assured — Gap Analysis

Based on analysis of ~80 REST Assured integration test files (~600+ test methods) from
[`examples/rest-assured-itest-java/src/test`](https://github.com/rest-assured/rest-assured/tree/master/examples/rest-assured-itest-java/src/test/java/io/restassured/itest/java).

---

## 1. Features & API We Don't Yet Support

### 1.1 Intentionally Out of Scope (won't implement)

These are Groovy/XML features that are explicitly excluded from http-assured's JSON-only, no-Groovy philosophy.

| Feature | REST Assured Tests | Rationale |
|---------|-------------------|-----------|
| XML/XPath/GPath for XML | `XMLGetITest` (41), `XMLPostITest` (11), `XPathITest` (6) | JSON-only for v1 (Decision #4) |
| XSD/DTD schema validation | `XMLValidationITest` (10), `GivenWhenThenXsdITest` (6) | XML out of scope |
| XML namespaces | `NamespaceExpectationsITest`, `NamespaceResponseParsingITest`, `GivenWhenThenNamespaceITest` (11 total) | XML out of scope |
| Groovy GPath closures | `JSONGetITest` — `findAll { book -> book.price > 10 }`, `size()`, `getAt()`, negative indexing `[-1]`, ranges `[0..2]` | Use JsonPath `$..book[?(@.price > 10)]` instead |
| Hamcrest matchers | All tests — `equalTo()`, `hasItems()`, `containsString()`, `hasXPath()`, etc. | Replaced by `BodyAssertion<T>` / `Assertions.*` (Decision #10) |
| `expect().when()` legacy syntax | `JSONGetITest`, `JSONPostITest`, many others | Only `given().when().then()` supported |
| Groovy `content()` alias for `body()` | `JSONGetITest` — tests 34-37 | Not needed, use `body()` |
| Static mutable global config | `RestAssured.baseURI`, `.port`, `.basePath`, `.authentication`, `.requestSpecification`, `.responseSpecification`, `.rootPath`, `.filters()`, `.defaultParser`, `.urlEncodingEnabled`, `.reset()` | Instance-based config by design (Decision #5) |
| JAXB / Gson / Jackson 1 mappers | `ObjectMappingITest`, `TypeObjectMappingITest`, `CustomObjectMappingITest` | Jackson 3 only; JSON-B via future SPI module |
| JSONP support | `JSONPGetITest` (1) | Niche, low priority |
| Apache HttpClient config | `HttpClientConfigITest` (7) | Uses Vert.x engine, not Apache HttpClient |
| `ResponseAwareMatcherITest` | (10) — lambda-based matchers referencing response paths | Could be implemented as `BodyAssertion` variant, but Groovy-flavored concept |
| HTML parsing | `ResponseITest` — `.htmlPath()`, `.xmlPath(HTML)` | HTML is XML variant, out of scope |

### 1.2 Features That Should Be Implemented

Grouped by priority/impact.

#### P1 — High Impact (common in real-world usage) — ALL IMPLEMENTED

| Feature | REST Assured API | Status | Differences with REST Assured |
|---------|-----------------|--------|-------------------------------|
| ~~Form parameters~~ | `formParam(name, value)`, `formParams(Map)` | **IMPLEMENTED** | Same API. Auto-sets `Content-Type: application/x-www-form-urlencoded` |
| ~~Multipart upload~~ | `multiPart(name, file)`, `multiPart(name, bytes, mimeType)` | **IMPLEMENTED** | Uses `MultiPartSpec` record factory methods instead of `MultiPartSpecBuilder`. No per-part headers or charset config |
| ~~Multi-value query params~~ | `queryParam(name, val1, val2)`, `queryParam(name, List)` | **IMPLEMENTED** | Same API. No `param()` alias — use `queryParam()` explicitly |
| ~~`accept()` method~~ | `accept(String)`, `accept(ContentType)` | **IMPLEMENTED** | Same API |
| ~~`body()` multi-path assertions~~ | `body("path1", matcher1, "path2", matcher2)` | **IMPLEMENTED** | Uses `BodyAssertion` instead of Hamcrest matchers |
| ~~Map as request body~~ | `body(Map)` serialized to JSON | **IMPLEMENTED** | Same API — `body(Object)` handles Maps via Jackson 3 |
| ~~`body(InputStream)` for request~~ | `given().body(inputStream)` | **IMPLEMENTED** | Same API — reads all bytes |
| ~~Multiple headers with same name~~ | `header("name", "v1", "v2")`, `Headers.getList()` | **IMPLEMENTED** | `Headers.getList(name)` returns `List<Header>` for multi-value access |
| ~~Response `headers()` multi-value~~ | `response.headers().getValues("name")` | **IMPLEMENTED** | Same API via `Headers.getValues()` and `Headers.getList()` |
| ~~Redirect control~~ | `redirects().follow(false)`, `redirects().max(int)` | **IMPLEMENTED** | Uses `RedirectSpec` record: `redirect(RedirectSpec.dontFollow())`, `redirect(RedirectSpec.maxRedirects(n))` instead of fluent `redirects().follow(false)` |

#### P2 — Medium Impact — 14/15 IMPLEMENTED

| Feature | REST Assured API | Status | Differences with REST Assured |
|---------|-----------------|--------|-------------------------------|
| ~~Status line assertion~~ | `statusLine("HTTP/1.1 200 OK")` | **IMPLEMENTED** | Uses `contains` match: `statusLine("200 OK")`. Format is `"<code> <message>"` without HTTP version prefix. On `Response`: `statusLine()` returns the string directly |
| ~~Response time~~ | `time()`, `timeIn(TimeUnit)` | **IMPLEMENTED** | Same API on `Response`. No `then().time(matcher)` assertion variant — use `response.time()` directly |
| ~~URL encoding control~~ | `urlEncodingEnabled(false)` | **IMPLEMENTED** | Same API: `urlEncodingEnabled(false)` on `RequestBuilder`. No global static toggle — per-request only |
| ~~No-value params~~ | `queryParam("key")` without value | **IMPLEMENTED** | Same API. Emits `?key` without `=value` |
| ~~`request(Method, path)` generic~~ | `request(Method.GET, "/path")`, `request("GET", "/path")` | **IMPLEMENTED** | Same API. Accepts `HttpMethod` enum or `String`. Supports path params via varargs |
| ~~`prettyPrint()` / `peek()` / `prettyPeek()`~~ | On `Response` — print and return for chaining | **IMPLEMENTED** | Same API. `prettyPrint()` and `prettyPeek()` format JSON. `peek()` prints raw body. No `print()` — use `peek()` instead |
| ~~Root path~~ | `rootPath("store")`, `appendRootPath()`, `detachRootPath()` | **IMPLEMENTED** | Same API on `ValidatableResponse`. No `withArgs()` parameterized root path variant. Paths starting with `$` bypass root path |
| ~~Detailed cookie attributes~~ | `detailedCookie("name")` with domain, path, maxAge, etc. | **IMPLEMENTED** | Same API on `Response`. `Cookie` uses builder pattern with `sameSite()` as String (not enum). No `getExpiryDate()` — use `maxAge()`. Default maxAge is `-1` (session cookie) |
| ~~Conditional logging~~ | `log().ifStatusCodeIsEqualTo(int)`, `log().ifStatusCodeMatches(Matcher)` | **IMPLEMENTED** | `ifStatusCodeMatches()` takes `IntPredicate` instead of Hamcrest `Matcher<Integer>` |
| Request log granularity | `log().params()`, `log().cookies()`, `log().method()`, `log().uri()` | **Not yet** | Currently only `log().all()`, `log().headers()`, `log().body()` on request side |
| ~~`and()` / `assertThat()` chaining sugar~~ | `.then().assertThat().statusCode(200).and().body(...)` | **IMPLEMENTED** | Same API — no-op methods returning `this` for readability |
| ~~`extract()` after assertions~~ | `.then().statusCode(200).extract("id")` | **IMPLEMENTED** | `extract(path)` directly on `ValidatableResponse` — no intermediate `.extract()` call. Also `extractAs(Class)`, `extractAs(TypeReference)` |
| ~~`asPrettyString()`~~ | `.body().asPrettyString()` | **IMPLEMENTED** | On `Response` directly: `response.asPrettyString()`. Falls back to raw body if not valid JSON |
| ~~`onFailMessage()`~~ | Custom assertion failure message | **IMPLEMENTED** | Same API on `ValidatableResponse`. Prepends custom message to `statusCode()`, `statusLine()`, and `body()` assertion failures |
| ~~`headers()` variadic assertion~~ | `.headers("Content-Type", "...", "X-Custom", "value")` | **IMPLEMENTED** | Same API. Alternating name/value pairs with odd-count rejection |

#### P3 — Lower Impact / Advanced — Not yet implemented

| Feature | REST Assured API | Tests | Notes |
|---------|-----------------|-------|-------|
| **Filter API** | `Filter`, `OrderedFilter`, `FilterableRequestSpecification` | `FilterITest` (32), `OrderedFilterITest` (2) | Interceptor pattern for request/response manipulation |
| **Proxy support** | `proxy(host, port)`, `proxy(ProxySpecification)` | `ProxyITest` (11), `ProxyAuthITest` (4) | Vert.x supports proxy via `ProxyOptions` |
| **Form authentication** | `auth().form(user, pass)`, `FormAuthConfig` | `AuthenticationITest` (30) | Multi-step: GET login page, POST credentials, follow redirect |
| **CSRF support** | `csrf(tokenPath)`, `CsrfConfig` | `CsrfITest` (25) | Fetch token from page, inject into POST |
| **Session management** | `SessionFilter`, `sessionId()`, `SessionConfig` | `SessionIdITest` (11) | Stateful session across requests |
| **Two-way SSL** | `keyStore()`, client certificate auth | `SSLITest` (24) | Client cert via Vert.x `KeyCertOptions` |
| **`relaxedHTTPSValidation()`** | Convenience for `trustAll` | `SSLITest` | We have `trustAll(true)` but no shorthand |
| **GZIP / DEFLATE** | Automatic decompression, `DecoderConfig` | `GzipITest` (2), `DecoderConfigITest` (8) | Vert.x may handle this automatically |
| **File download** | `asInputStream()` for large responses | `FileDownloadITest` (3, disabled) | `bodyAsBytes()` exists but no streaming |
| **Param merge/replace strategies** | `ParamConfig`, `UpdateStrategy.REPLACE` | `ParamConfigITest` (8) | Duplicate param handling |
| **`HeaderConfig`** | Merge vs overwrite headers with same name | `HeaderConfigITest` (6) | Currently `Headers.with()` always replaces |
| **`JsonConfig`** | `numberReturnType(BIG_DECIMAL)` | `BigDecimalITest` (3), `DoubleITest` (3) | Control JSON number types |
| **`FailureConfig`** | `ResponseValidationFailureListener` | `FailureConfigITest` (1) | Callback on assertion failure |
| **Custom parser registration** | `registerParser(contentType, Parser)` | `DefaultParserITest` (4), `ParserITest` (1) | Treat custom MIME types as JSON |
| **BOM handling** | UTF BOM in responses | `BomITest` (2) | Strip BOM before parsing |
| **Logging to disk** | Custom `OutputStream` for logs | `LoggingToDiskTest` (3) | File-based log output |
| **Header mapping function** | `header("Content-Length", Integer::parseInt, lessThan(200))` | `HeaderITest` (tests 2-3) | Transform header value before assertion |
| **`EncoderConfig`** | `defaultContentCharset()`, `encodeContentTypeAs()` | `ContentTypeITest` (40) | Request encoding configuration |
| **`noContentType()`** | Suppress automatic Content-Type header | `ContentTypeITest` | Prevent auto-set of Content-Type |

---

## 2. Differences with REST Assured (Migration Guide)

### 2.1 Fundamental Architecture

| Aspect | REST Assured | http-assured |
|--------|-------------|-------------|
| **Configuration** | Static mutable globals (`RestAssured.baseURI = ...`) | Instance-based builder (`HttpAssured.builder().baseUri(...).build()`) |
| **HTTP engine** | Apache HttpClient 4.x | Vert.x 5.1.3 WebClient |
| **JSON library** | Jackson 2 (com.fasterxml) | Jackson 3 (tools.jackson) |
| **JSON querying** | Groovy GPath (default) + JsonPath | Jayway JsonPath only |
| **Assertion library** | Hamcrest matchers | JUnit 5 assertions via `BodyAssertion<T>` |
| **XML support** | Full XML/XPath/XSD/DTD | JSON only (by design) |
| **Groovy dependency** | Required (core uses Groovy GPath) | None |
| **Logging** | SLF4J / custom `PrintStream` | JBoss Logging |
| **Reactive support** | None | Mutiny `Uni<RawResponse>` on engine |

### 2.2 API Mapping — What Changes

| REST Assured | http-assured | Notes |
|-------------|-------------|-------|
| `RestAssured.baseURI = "..."` | `HttpAssured.builder().baseUri("...").build()` | No global state |
| `RestAssured.port = 8080` | `.port(8080)` on builder | |
| `RestAssured.basePath = "/api"` | `.basePath("/api")` on builder | |
| `given().param("k","v")` | `given().queryParam("k","v")` | No `param()` alias; explicit `queryParam` |
| `body("path", equalTo("x"))` | `body("path", isEqualTo("x"))` | `Assertions.isEqualTo()` replaces `Matchers.equalTo()` |
| `body("path", hasItems(1,2))` | `body("path", containsAll(1,2))` | `containsAll` replaces `hasItems` |
| `body("path", hasSize(3))` | `body("path", hasSize(3))` | Same name, different import |
| `body("path", containsString("x"))` | `body("path", containsString("x"))` | Same name, different import |
| `body("path", greaterThan(5))` | `body("path", greaterThan(5))` | Same name, different import |
| `body("path", is(nullValue()))` | `body("path", isNull())` | |
| `body("path", not(nullValue()))` | `body("path", isNotNull())` | |
| `body(containsString("hello"))` | `bodyContains("hello")` | Whole-body assertion is a different method |
| `body(equalTo("exact"))` | `bodyEquals("exact")` | |
| `.contentType(ContentType.JSON)` | `.contentType(ContentType.JSON)` | Same API, different import |
| `extract().path("id")` | `extract("id")` | Shorter — directly on `ValidatableResponse` |
| `.as(MyClass.class)` | `.extractAs(MyClass.class)` or `.bodyAs(MyClass.class)` | Different method name |
| `.asString()` | `.bodyAsString()` | On `Response` |
| `.asByteArray()` | `.bodyAsBytes()` | On `Response` |
| `.asPrettyString()` | `.asPrettyString()` | Same API on `Response` |
| `response.header("Name")` | `response.getHeader("Name")` | Or `response.headers().getValue("Name")` returns `Optional` |
| `response.statusLine()` | `response.statusLine()` | Same API. Returns `"<code> <message>"` (no HTTP version prefix) |
| `response.time()` | `response.time()` | Same API. Returns milliseconds |
| `response.timeIn(TimeUnit)` | `response.timeIn(TimeUnit)` | Same API |
| `response.prettyPrint()` | `response.prettyPrint()` | Same API — prints and returns `Response` |
| `response.peek()` | `response.peek()` | Same API — prints raw body, returns `Response` |
| `response.prettyPeek()` | `response.prettyPeek()` | Same API |
| `response.detailedCookie("name")` | `response.detailedCookie("name")` | Same API. Cookie has `sameSite()` (String), no `getExpiryDate()` |
| `response.cookie("name")` | Not on `Response` — assert via `.then().cookie(name, value)` | Cookies are on validation side only |
| `response.cookies()` | Not on `Response` directly | |
| `.relaxedHTTPSValidation()` | `.trustAll(true)` | Per-request, not global |
| `auth().basic(u, p)` | `auth().basic(u, p)` | Same API |
| `auth().preemptive().basic(u, p)` | `auth().preemptive().basic(u, p)` | Same API |
| `auth().oauth2(token)` | `auth().oauth2(token)` | Same API |
| `auth().oauth(ck, cs, at, ts)` | `auth().oauth(ck, cs, at, ts)` | Same API |
| `multiPart(name, file)` | `multiPart(name, file)` | Same basic API; no `MultiPartSpecBuilder` |
| `MultiPartSpecBuilder` | `MultiPartSpec.file(...)`, `MultiPartSpec.bytes(...)`, `MultiPartSpec.text(...)` | Record factory methods instead of builder. No per-part headers/charset |
| `redirects().follow(false)` | `redirect(RedirectSpec.dontFollow())` | Record-based instead of fluent chain |
| `redirects().max(5)` | `redirect(RedirectSpec.maxRedirects(5))` | Record-based |
| `urlEncodingEnabled(false)` | `urlEncodingEnabled(false)` | Same API, per-request only |
| `request(Method.GET, "/path")` | `request(HttpMethod.GET, "/path")` | Same API, different enum import |
| `request("POST", "/path")` | `request("POST", "/path")` | Same API |
| `matchesJsonSchemaInClasspath("x.json")` | `matchesJsonSchema("x.json")` | Classpath is the default source |
| `log().all()` | `log().all()` | Same API on both request and response |
| `log().ifValidationFails()` | `log().ifValidationFails()` | Same API |
| `log().ifStatusCodeIsEqualTo(n)` | `log().ifStatusCodeIsEqualTo(n)` | Same API |
| `log().ifStatusCodeMatches(matcher)` | `log().ifStatusCodeMatches(predicate)` | Takes `IntPredicate` instead of Hamcrest `Matcher` |
| `spec(requestSpec)` | `spec(requestSpec)` | Same API |
| `.and()` | `.and()` | Same API — no-op chaining sugar |
| `.assertThat()` | `.assertThat()` | Same API — no-op chaining sugar |
| `.rootPath("store")` | `.rootPath("store")` | Same API on `ValidatableResponse` |
| `.appendRootPath("book")` | `.appendRootPath("book")` | Same API |
| `.detachRootPath()` | `.detachRootPath()` | Same API. No `detachRootPath(path)` partial detach variant |
| `.rootPath("store.book[%d]").withArgs(0)` | Not supported | No parameterized root path |
| `.onFailMessage("custom msg")` | `.onFailMessage("custom msg")` | Same API |
| `.headers("CT", "json", "X", "v")` | `.headers("CT", "json", "X", "v")` | Same API — variadic name/value pairs |
| `queryParam("key")` (no value) | `queryParam("key")` (no value) | Same API — emits `?key` |
| `formParam("k", "v")` | `formParam("k", "v")` | Same API |
| `accept("application/json")` | `accept("application/json")` | Same API |
| `expect().body(...).when().get(...)` | Not supported | Legacy syntax — use `given().when().get(...).then().body(...)` |
| `then().time(lessThan(5000L))` | Not supported | No time assertion — use `response.time()` directly |

### 2.3 JsonPath Syntax Differences

| REST Assured (GPath) | http-assured (Jayway JsonPath) | Notes |
|---------------------|-------------------------------|-------|
| `lotto.lottoId` | `lotto.lottoId` or `$.lotto.lottoId` | Simple paths work the same |
| `lotto.winners[0].winnerId` | `lotto.winners[0].winnerId` | Array indexing is the same |
| `store.book[-1].title` | `$.store.book[-1:].title` | Negative indexing syntax differs |
| `store.book[0,1].title` | `$.store.book[0,1].title` | Multiple indices — use `$` prefix |
| `store.book[0..2]` | Not supported | Ranges are Groovy-specific |
| `store.book.findAll { it.price > 10 }` | `$.store.book[?(@.price > 10)]` | Filter syntax differs |
| `store.book.size()` | `$.store.book.length()` | Size/length function name |
| `store.book.getAt(0)` | `$.store.book[0]` | No `getAt()` — use bracket notation |
| `body.'@id'` | `body.@id` or `$['body']['@id']` | Special character escaping differs |
| `store.book*.author` | `$.store.book[*].author` | Spread operator syntax |

### 2.4 Import Changes

```java
// REST Assured
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

// http-assured
import static io.smallrye.httpassured.assertion.Assertions.isEqualTo;
import static io.smallrye.httpassured.assertion.Assertions.containsAll;
// HttpAssured instance — no static import needed
```

---

## 3. Tests We Already Support

### 3.1 Already Implemented in `compat/`

| Compat Test | Covers |
|------------|--------|
| `FloatExampleTest` | Float JSON values, JsonPath |
| `HeadersParametersTest` | Headers and query parameters |
| `HttpsUntrustedCertTest` | TLS with `trustAll(true)` |
| `JsonAssertionTest` | JSON body assertions, JsonPath |
| `JsonSchemaValidationTest` | JSON Schema from classpath, string, stream, URI |
| `LottoExampleTest` | Nested JSON, array assertions |
| `ResponseExtractionTest` | `extract()`, `extractAs()`, `bodyAs()` |
| `RestAssuredTutorialTest` | GET/POST/PUT/DELETE basic flows |
| `RestAssuredWikiAuthTest` | Basic, preemptive basic, OAuth 2, OAuth 1 |
| `RestAssuredWikiCookieTest` | Cookie send/assert |
| `SchemaExampleTest` | JSON Schema validation |
| `MultiPartUploadTest` | File upload, byte-array upload, text fields, auto Content-Type |
| `AcceptHeaderTest` | `accept()` method |
| `FormParamTest` | Form parameters with URL-encoded body |
| `MultiValueHeaderTest` | Multi-value header access (`Headers.getList()`) |
| `MultiPathBodyAssertionTest` | Multi-path body assertions in single call |
| `RedirectTest` | Redirect control with `RedirectSpec` |
| `RequestBodyTest` | Map, InputStream, byte[] request bodies |
| `JsonGetTest` | Simple JSON GET, nested paths, array indexing |
| `JsonPostTest` | POST with JSON body, string body, object serialization |
| `PutTest` | PUT with JSON body |
| `PatchTest` | PATCH with JSON body, Map body |
| `DeleteTest` | DELETE with status code assertion |
| `HeadTest` | HEAD request, verify empty body |
| `OptionsTest` | OPTIONS with header assertion |
| `GivenWhenThenTest` | BDD syntax, multiple body assertions |
| `ExtractTest` | `extract()`, `extractAs()`, extract after assertion |
| `HeaderTest` | Single/multiple headers, header assertion |
| `ResponseTest` | `statusCode()`, `bodyAsString()`, `bodyAs()`, `headers()` |
| `SpecBuilderTest` | `RequestSpec`, `ResponseSpec`, spec merging |
| `ContentTypeTest` | Content-type assertion |
| `LoggingTest` | `log().all()`, `log().body()`, `log().headers()`, `log().ifValidationFails()` |
| `SslTest` | `trustAll(true)`, `trustOptions(TrustOptions.pem/jks/pkcs12)` |
| `PathParamTest` | Named and positional path params |
| `UrlTest` | Base URI, port, base path, slash handling |
| `ObjectMappingTest` | `bodyAs(Class)`, `body(Object)`, `extractAs(TypeReference)` |
| `CookieTest` | `cookie(name, value)`, multi-value cookies |
| `UnicodeTest` | UTF-8 in JSON bodies and paths |
| `P2ValidatableResponseTest` | statusLine, rootPath, appendRootPath, detachRootPath, and/assertThat, onFailMessage, variadic headers |
| `P2ResponseFeaturesTest` | prettyPrint, peek, prettyPeek, asPrettyString, statusLine, time, timeIn, detailedCookie, extract after assertions |
| `P2RequestFeaturesTest` | URL encoding control, no-value params, request(HttpMethod), request(String), ifStatusCodeIsEqualTo, ifError, ifStatusCodeMatches |

### 3.2 Test Count Summary

| Category | REST Assured Tests | Ported | Still Need Features | Out of Scope |
|----------|-------------------|--------|---------------------|-------------|
| JSON GET/POST | ~140 | ~110 | ~0 | ~30 (GPath, Hamcrest-specific) |
| PUT/PATCH/DELETE/HEAD/OPTIONS | ~20 | ~20 | ~0 | 0 |
| Headers | ~30 | ~30 | ~0 (HeaderConfig, mapping function are P3) | 0 |
| Cookies | ~33 | ~25 | ~8 (response cookies map) | 0 |
| Authentication | ~35 | ~10 | ~25 (form auth, CSRF, session — P3) | 0 |
| SSL/TLS | ~24 | ~8 | ~16 (two-way SSL — P3) | 0 |
| Logging | ~90 | ~30 | ~60 (granular request logging — P2 remaining) | 0 |
| Specs/Config | ~40 | ~15 | ~25 (filter, ParamConfig — P3) | 0 |
| Object Mapping | ~30 | ~10 | ~0 | ~20 (JAXB/Gson/Jackson1) |
| URL/Params | ~80 | ~70 | ~10 (param merge strategies — P3) | 0 |
| XML/XPath/Namespaces | ~75 | 0 | 0 | ~75 (all XML out of scope) |
| File Upload/Download | ~47 | ~10 | ~37 (MultiPartSpecBuilder, download — P3) | 0 |
| Encoding/Unicode | ~20 | ~8 | ~12 (GZIP, BOM, charset config — P3) | 0 |
| Misc (proxy, redirect, etc.) | ~35 | ~10 | ~25 (proxy, filters — P3) | 0 |
| **TOTALS** | **~700** | **~356** | **~218** | **~125** |
