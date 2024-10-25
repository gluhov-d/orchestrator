package com.github.gluhov.orchestrator.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.rest.UserRestControllerV1;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.gluhov.orchestrator.rest.AuthRestControllerV1.REST_URL;
import static com.github.gluhov.orchestrator.service.IndividualsData.INDIVIDUAL_ID;
import static com.github.gluhov.orchestrator.service.IndividualsData.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "36000")
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 8181)
public class ItUserRestControllerV1Test extends AbstractRestControllerTest{
    @Autowired
    private WebTestClient webTestClient;
    private String accessToken;

    @BeforeEach
    public void setUp() {
        if (accessToken == null) {
            getToken();
        }
    }

    private void getToken() {
        AuthRequestDto authRequest = AuthRequestDto.builder()
                .username("user")
                .password("password")
                .build();

        String resp = webTestClient.post()
                .uri(REST_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(resp);
            JsonNode accessTokenNode = rootNode.path("body").path("access_token");
            if (accessTokenNode.isMissingNode()) {
                throw new RuntimeException("Access token not found in response");
            }
            accessToken = accessTokenNode.asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parsing token exception");
        }
    }

    @Test
    @DisplayName("Test get user info then success response")
    public void givenAccessToken_whenGetInfo_thenSuccessResponse() {
        stubFor(WireMock.get(urlPathMatching("/api/v1/individuals/" + INDIVIDUAL_ID + "/details"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        WebTestClient.ResponseSpec result = webTestClient.get()
                .uri(UserRestControllerV1.REST_URL + "/" + INDIVIDUAL_ID + "/profile")
                .header("Authorization", "Bearer " + accessToken)
                .exchange();

        result.expectStatus().isOk()
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.body.email").isEqualTo("test@example.com")
                .jsonPath("$.body.phone_number").isEqualTo("1234-553-222")
                .jsonPath("$.body.passport_number").isEqualTo("479-80-111")
                .jsonPath("$.body.user.first_name").isEqualTo("Jon")
                .jsonPath("$.body.user.last_name").isEqualTo("Will");
    }

    @Test
    @DisplayName("Test get user info then wrong access token")
    public void givenAccessToken_whenGetInfo_thenWrongAccessTokenResponse() {
        WebTestClient.ResponseSpec result = webTestClient.get()
                .uri(UserRestControllerV1.REST_URL + "/" + INDIVIDUAL_ID + "/profile")
                .header("Authorization", "Bearer fyugugyikk")
                .exchange();

        result.expectStatus().is4xxClientError();
    }
}