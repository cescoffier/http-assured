package io.smallrye.httpassured.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An immutable collection of HTTP headers.
 * Supports multiple values for the same header name.
 */
public final class Headers implements Iterable<Header> {

    private final List<Header> headers;

    public Headers(List<Header> headers) {
        this.headers = List.copyOf(headers);
    }

    public Headers() {
        this.headers = List.of();
    }

    /**
     * Returns the first value for the given header name, if present.
     */
    public Optional<String> getValue(String name) {
        return headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .map(Header::value)
                .findFirst();
    }

    /**
     * Returns all values for the given header name.
     */
    public List<String> getValues(String name) {
        return headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .map(Header::value)
                .toList();
    }

    /**
     * Returns all {@link Header} objects with the given name.
     *
     * @param name the header name (case-insensitive)
     * @return an unmodifiable list of matching headers; empty if none match
     */
    public List<Header> getList(String name) {
        return headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .toList();
    }

    /**
     * Returns true if a header with the given name exists.
     */
    public boolean hasHeader(String name) {
        return headers.stream().anyMatch(h -> h.name().equalsIgnoreCase(name));
    }

    /**
     * Returns all headers as an unmodifiable list.
     */
    public List<Header> asList() {
        return headers;
    }

    public int size() {
        return headers.size();
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }

    /**
     * Creates a new Headers instance by adding a header to this collection.
     */
    public Headers with(String name, String value) {
        List<Header> merged = new ArrayList<>(headers);
        merged.add(new Header(name, value));
        return new Headers(merged);
    }

    /**
     * Creates a new Headers instance by merging another Headers collection.
     */
    public Headers merge(Headers other) {
        List<Header> merged = new ArrayList<>(headers);
        merged.addAll(other.headers);
        return new Headers(merged);
    }

    /**
     * Creates a new Headers instance by replacing all headers with the given name.
     * <p>
     * Any existing headers matching the name (case-insensitive) are removed,
     * and a single new header with the given name and value is added.
     * </p>
     *
     * @param name  the header name
     * @param value the header value
     * @return a new Headers instance with the header replaced
     */
    public Headers replacing(String name, String value) {
        List<Header> filtered = headers.stream()
                .filter(h -> !h.name().equalsIgnoreCase(name))
                .collect(Collectors.toCollection(ArrayList::new));
        filtered.add(new Header(name, value));
        return new Headers(filtered);
    }
}
