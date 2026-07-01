package io.smallrye.httpassured.integration.compat;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import io.smallrye.httpassured.mapper.jackson.JacksonObjectMapperProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@WireMockTest
public class FloatExampleTest {

    private HttpAssured client;
    private HttpAssured clientWithMapper;


    @BeforeEach
    void setupClient(WireMockRuntimeInfo wmInfo) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build();
        clientWithMapper = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .objectMapper(new JacksonObjectMapperProvider(mapper))
                .build();

        stubFor(get(urlEqualTo("/price"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                price: 12.12
                                }
                                """)));
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.close();
        }
        if (clientWithMapper != null) {
            clientWithMapper.close();
        }
    }

    @Test
    void testDoubleExample() {
        // REST Assured used a float, we use double.
        client.given().get("/price").then().body("price", Assertions.isEqualTo(12.12));
        clientWithMapper.given().get("/price").then().body("price", Assertions.isEqualTo(new BigDecimal("12.12")));
    }




}
