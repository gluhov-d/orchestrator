package com.github.gluhov.orchestrator.service;

import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.AuthResponseDto;
import com.github.gluhov.orchestrator.dto.RefreshTokenRequestDto;
import com.github.gluhov.orchestrator.exception.ApiException;
import com.github.gluhov.orchestrator.exception.AuthException;
import com.github.gluhov.orchestrator.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SecurityServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(securityService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(securityService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(securityService, "keycloakIssuerUri", "http://localhost:8088/realms/test-realm/");
    }

    @Test
    @DisplayName("Test authenticate user with correct credentials")
    void authenticateSuccess() {
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
                .username("test@ya.ru")
                .password("password")
                .build();
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setToken("token");
        accessTokenResponse.setExpiresIn(3600);
        accessTokenResponse.setRefreshToken("refreshToken");
        accessTokenResponse.setTokenType("Bearer");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AccessTokenResponse.class)).thenReturn(Mono.just(accessTokenResponse));

        Mono<AuthResponseDto> result = securityService.authenticate(authRequestDto);

        StepVerifier.create(result)
                .expectNextMatches(authResponseDto ->
                        authResponseDto.getAccessToken().equals("token") &&
                                authResponseDto.getRefreshToken().equals("refreshToken") &&
                                authResponseDto.getExpiresIn() == 3600
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Test authenticate user with wrong credentials")
    void authenticateError() {
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
                .username("test2@ya.ru")
                .password("password").build();

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(new AuthException("Error from Keycloak", "O_AUTHENTICATE_ERROR")));

        Mono<AuthResponseDto> result = securityService.authenticate(authRequestDto);

        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AuthException);
                    assertEquals("Error from Keycloak", error.getMessage());
                    assertEquals("O_AUTHENTICATE_ERROR", ((AuthException) error).getErrorCode());
                })
                .verify();
    }

    @Test
    @DisplayName("Test refresh token with valid token")
    void refreshTokenSuccess() {
        RefreshTokenRequestDto refreshTokenRequestDto = RefreshTokenRequestDto.builder()
                .refreshToken("eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJiZDVjYWJjNS1lMmI2LTQyYzYtYjI2My0yZmNiMWU4ZmI3ZDYifQ.")
                .build();
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setToken("token");
        accessTokenResponse.setExpiresIn(3600);
        accessTokenResponse.setRefreshToken("refreshToken");
        accessTokenResponse.setTokenType("Bearer");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AccessTokenResponse.class)).thenReturn(Mono.just(accessTokenResponse));

        Mono<AuthResponseDto> result = securityService.refreshToken(refreshTokenRequestDto);

        StepVerifier.create(result)
                .expectNextMatches(authResponseDto ->
                        authResponseDto.getAccessToken().equals("token") &&
                                authResponseDto.getRefreshToken().equals("refreshToken") &&
                                authResponseDto.getExpiresIn() == 3600
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Test refresh token with wrong token")
    void refreshTokenError() {
        RefreshTokenRequestDto refreshTokenRequestDto = RefreshTokenRequestDto.builder()
                .refreshToken("eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJiZDVjYWJjNS1lMmI2LTQyYzYtYjI2My0yZmNiMWU4ZmI3ZDYifQ.")
                .build();
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(new AuthException("Error from Keycloak", "O_REFRESH_TOKEN_ERROR")));

        Mono<AuthResponseDto> result = securityService.refreshToken(refreshTokenRequestDto);

        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AuthException);
                    assertEquals("Error from Keycloak", error.getMessage());
                    assertEquals("O_REFRESH_TOKEN_ERROR", ((ApiException) error).getErrorCode());
                })
                .verify();
    }
}