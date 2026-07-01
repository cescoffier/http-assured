package io.smallrye.httpassured.http;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * An immutable collection of {@link Cookie} instances, used to send several
 * cookies in one call via {@code given().cookies(cookies)}.
 *
 * <pre>{@code
 * Cookie c1 = new Cookie.Builder("username", "John").comment("user cookie").build();
 * Cookie c2 = new Cookie.Builder("token", "1234").comment("auth token").build();
 * Cookies cookies = new Cookies(c1, c2);
 * client.given().cookies(cookies).when().get("/cookie").then().statusCode(200);
 * }</pre>
 */
public final class Cookies implements Iterable<Cookie> {

    private final List<Cookie> cookies;

    /**
     * Creates a {@code Cookies} collection from the given cookies.
     *
     * @param cookies one or more cookies (must not be null)
     */
    public Cookies(Cookie... cookies) {
        Objects.requireNonNull(cookies, "Cookies array must not be null");
        this.cookies = List.copyOf(Arrays.asList(cookies));
    }

    /**
     * Creates a {@code Cookies} collection from a list of cookies.
     *
     * @param cookies the cookie list (must not be null)
     */
    public Cookies(List<Cookie> cookies) {
        Objects.requireNonNull(cookies, "Cookies list must not be null");
        this.cookies = List.copyOf(cookies);
    }

    /** Returns the number of cookies in this collection. */
    public int size() {
        return cookies.size();
    }

    /** Returns the cookies as an unmodifiable list. */
    public List<Cookie> asList() {
        return cookies;
    }

    @Override
    public Iterator<Cookie> iterator() {
        return cookies.iterator();
    }
}
