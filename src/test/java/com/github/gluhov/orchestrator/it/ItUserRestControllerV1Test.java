package com.github.gluhov.orchestrator.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.rest.UserRestControllerV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.gluhov.orchestrator.rest.AuthRestControllerV1.REST_URL;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
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
    @DisplayName("Test get user info")
    public void givenAccessToken_whenGetInfo_thenSuccessResponse() {
        WebTestClient.ResponseSpec result = webTestClient.get()
                .uri(UserRestControllerV1.REST_URL)
                .header("Authorization", "Bearer " + accessToken)
                .exchange();

        result.expectStatus().isOk()
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.body.email").isEqualTo("example@keycloak.org")
                .jsonPath("$.body.username").isEqualTo("Example User")
                .jsonPath("$.body.first_name").isEqualTo("Example")
                .jsonPath("$.body.last_name").isEqualTo("User");
    }

    @Test
    @DisplayName("Test get user info then wrong access token")
    public void givenAccessToken_whenGetInfo_thenWrongAccessTokenResponse() {
        WebTestClient.ResponseSpec result = webTestClient.get()
                .uri(UserRestControllerV1.REST_URL)
                .header("Authorization", "Bearer fyugugyikk")
                .exchange();

        result.expectStatus().is4xxClientError();
    }
}