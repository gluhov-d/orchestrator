package com.github.gluhov.orchestrator.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.RefreshTokenRequestDto;
import com.github.gluhov.orchestrator.rest.AuthRestControllerV1;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.gluhov.orchestrator.service.IndividualsData.individualsDto;
import static com.github.gluhov.orchestrator.service.IndividualsData.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "36000")
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 8181)
public class ItAuthRestControllerV1Test extends AbstractRestControllerTest{
    private final String REST_URL = AuthRestControllerV1.REST_URL;
    @Autowired
    private WebTestClient webTestClient;
    private String accessToken;
    private String refreshToken;
    @Value("${kc.user}")
    private String user;
    @Value("${kc.password}")
    private String password;

    @BeforeEach
    public void setUp() {
        if (accessToken == null || refreshToken == null) {
            getTokens();
        }
    }

    private void getTokens() {
        AuthRequestDto authRequest = AuthRequestDto.builder()
                .username(user)
                .password(password)
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
            JsonNode refreshTokenNode = rootNode.path("body").path("refresh_token");
            if (refreshTokenNode.isMissingNode()) {
                throw new RuntimeException("Refresh token not found in response");
            }
            accessToken = accessTokenNode.asText();
            refreshToken = refreshTokenNode.asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parsing token exception");
        }
    }

    @Test
    @DisplayName("Test login with correct credentials")
    public void givenCredentials_whenLogin_thenSuccessResponse() {
        AuthRequestDto authRequest = AuthRequestDto.builder()
                .username(user)
                .password(password)
                .build();

        WebTestClient.ResponseSpec result = webTestClient.post()
                .uri(REST_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange();

        result.expectStatus().isOk()
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.body.access_token").isNotEmpty()
                .jsonPath("$.body.refresh_token").isNotEmpty()
                .jsonPath("$.body.expires_in").isEqualTo(300);
    }

    @Test
    @DisplayName("Test register with correct data")
    public void givenUserData_whenRegister_thenSuccessResponse() {
        stubFor(WireMock.post(urlPathMatching("/api/v1/individuals"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        WebTestClient.ResponseSpec result = webTestClient.post()
                .uri(REST_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(individualsDto)
                .exchange();

        result.expectStatus().isOk()
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.body.passport_number").isEqualTo(individualsDto.getPassportNumber())
                .jsonPath("$.body.phone_number").isEqualTo(individualsDto.getPhoneNumber())
                .jsonPath("$.body.email").isEqualTo(individualsDto.getEmail());
    }

    @Test
    @DisplayName("Test register with invalid data")
    public void givenUserData_whenRegister_thenErrorResponse() {
        stubFor(WireMock.post(urlPathMatching("/api/v1/individuals"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{message: Bad Request}")));

        WebTestClient.ResponseSpec result = webTestClient.post()
                .uri(REST_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(individualsDto)
                .exchange();

        result.expectStatus().is4xxClientError()
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.errors[0].code").isEqualTo("O_REGISTER_USER_ERROR")
                .jsonPath("$.errors[0].message").isEqualTo("Failed to register user");
    }

    @Test
    @DisplayName("Test refresh token with invalid token")
    public void givenRefreshToken_whenRefreshToken_thenErrorResponse() {
        RefreshTokenRequestDto refreshTokenRequest = RefreshTokenRequestDto.builder()
                .refreshToken("refreshToken")
                .build();

        WebTestClient.ResponseSpec result = webTestClient.post()
                .uri(REST_URL + "/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refreshTokenRequest)
                .exchange();

        result.expectStatus().is4xxClientError()
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.errors[0].code").isEqualTo("O_REFRESH_TOKEN_ERROR")
                .jsonPath("$.errors[0].message").isEqualTo("Error from Keycloak: {\"error\":\"invalid_grant\",\"error_description\":\"Invalid refresh token\"}");
    }

    @Test
    @DisplayName("Test refresh token with valid token")
    public void givenRefreshToken_whenRefreshToken_thenSuccessResponse() {
        RefreshTokenRequestDto refreshTokenRequest = RefreshTokenRequestDto.builder()
                .refreshToken(refreshToken)
                .build();

        WebTestClient.ResponseSpec result = webTestClient.post()
                .uri(REST_URL + "/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refreshTokenRequest)
                .exchange();

        result.expectStatus().isOk()
                .expectBody()
                .consumeWith(System.out::println)
                .jsonPath("$.body.access_token").isNotEmpty()
                .jsonPath("$.body.refresh_token").isNotEmpty()
                .jsonPath("$.body.token_type").isEqualTo("Bearer");
    }
}