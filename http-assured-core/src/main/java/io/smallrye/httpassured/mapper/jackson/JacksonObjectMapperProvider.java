package io.smallrye.httpassured.mapper.jackson;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import io.smallrye.httpassured.HttpAssuredException;
import io.smallrye.httpassured.spi.ObjectMapperProvider;

import java.lang.reflect.Type;

/**
 * Jackson-based {@link ObjectMapperProvider} implementation.
 * <p>
 * Uses a sensible default configuration suitable for testing:
 * <ul>
 *   <li>Unknown properties are ignored during deserialization</li>
 *   <li>Empty beans serialization is allowed</li>
 * </ul>
 * </p>
 */
public record JacksonObjectMapperProvider(ObjectMapper objectMapper) implements ObjectMapperProvider {

    /**
     * Creates a provider with sensible defaults.
     */
    public JacksonObjectMapperProvider() {
        this(createDefaultMapper());
    }

    /**
     * Creates a provider with a custom ObjectMapper.
     */
    public JacksonObjectMapperProvider {
    }

    @Override
    public byte[] serialize(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (Exception e) {
            throw new HttpAssuredException("Failed to serialize object: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(byte[] body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (Exception e) {
            throw new HttpAssuredException(
                    "Failed to deserialize to " + type.getName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Object deserialize(byte[] body, Type type) {
        try {
            var javaType = objectMapper.getTypeFactory().constructType(type);
            return objectMapper.readValue(body, javaType);
        } catch (Exception e) {
            throw new HttpAssuredException(
                    "Failed to deserialize to " + type.getTypeName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns the underlying ObjectMapper for advanced configuration.
     */
    @Override
    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    private static ObjectMapper createDefaultMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
    }
}
