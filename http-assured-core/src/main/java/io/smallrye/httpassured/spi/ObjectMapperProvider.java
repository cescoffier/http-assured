package io.smallrye.httpassured.spi;

import java.lang.reflect.Type;

/**
 * SPI for object serialization/deserialization.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 * The default implementation uses Jackson.
 * </p>
 */
public interface ObjectMapperProvider {

    /**
     * Serializes an object to a byte array.
     *
     * @param object the object to serialize
     * @return the serialized bytes (typically JSON)
     */
    byte[] serialize(Object object);

    /**
     * Deserializes a byte array to an object of the given type.
     *
     * @param body the raw bytes
     * @param type the target class
     * @param <T> the target type
     * @return the deserialized object
     */
    <T> T deserialize(byte[] body, Class<T> type);

    /**
     * Deserializes a byte array to an object of the given generic type.
     *
     * @param body the raw bytes
     * @param type the target type (may be parameterized)
     * @return the deserialized object
     */
    Object deserialize(byte[] body, Type type);
}
