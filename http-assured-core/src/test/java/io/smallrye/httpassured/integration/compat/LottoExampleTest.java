package io.smallrye.httpassured.integration.compat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.smallrye.httpassured.HttpAssured;
import io.smallrye.httpassured.assertion.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@WireMockTest
public class LottoExampleTest {

    private HttpAssured client;


    @BeforeEach
    void setupClient(WireMockRuntimeInfo wmInfo) {
        client = HttpAssured.builder()
                .baseUri("http://localhost")
                .port(wmInfo.getHttpPort())
                .build();

        stubFor(get(urlEqualTo("/lotto"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                "lotto":{
                                 "lottoId":5,
                                 "winning-numbers":[2,45,34,23,7,5,3],
                                 "winners":[{
                                   "winnerId":23,
                                   "numbers":[2,45,34,23,3,5]
                                 },{
                                   "winnerId":54,
                                   "numbers":[52,3,12,11,18,22]
                                 }]
                                }
                                }
                                """)));
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testLottoExample() {
        client.given().get("/lotto").then().body("lotto.lottoId", Assertions.isEqualTo(5));
        // RESTAssured used lotto.winners.winnerId
        client.given().get("/lotto").then().body("lotto.winners[*].winnerId", Assertions.containsAll(23, 54));
    }

    @Test
    void testGivenWhenThen() {
        client.given().trustAll(true)
                .when().get("/lotto")
                .then().body("lotto.lottoId", Assertions.isEqualTo(5));
    }


}
