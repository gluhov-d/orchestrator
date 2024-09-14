package com.github.gluhov.orchestrator.security;

import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.AuthResponseDto;
import com.github.gluhov.orchestrator.dto.RefreshTokenRequestDto;
import com.github.gluhov.orchestrator.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityService {
    private final WebClient webClient;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String keycloakIssuerUri;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    public Mono<AuthResponseDto> authenticate(AuthRequestDto authRequestDto) {
        log.debug("try to login {}", authRequestDto);
        return webClient.post()
                .uri(keycloakIssuerUri + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", OAuth2Constants.PASSWORD)
                        .with("scope", "openid profile")
                        .with("username", authRequestDto.getUsername())
                        .with("password", authRequestDto.getPassword()))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error from Keycloak: {}", errorBody);
                                    return Mono.error(new ApiException("Error from Keycloak: " + errorBody, "O_REGISTRATION_ERROR"));
                                }))
                .bodyToMono(AccessTokenResponse.class)
                .map(accessTokenResponse -> AuthResponseDto.builder()
                        .accessToken(accessTokenResponse.getToken())
                        .expiresIn(accessTokenResponse.getExpiresIn())
                        .refreshToken(accessTokenResponse.getRefreshToken())
                        .tokenType(accessTokenResponse.getTokenType())
                        .build());
    }

    public Mono<AuthResponseDto> refreshToken(RefreshTokenRequestDto refreshTokenRequestDto) {
        return webClient.post()
                .uri(keycloakIssuerUri + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", OAuth2Constants.REFRESH_TOKEN)
                        .with("refresh_token", refreshTokenRequestDto.getRefreshToken()))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error from Keycloak: {}", errorBody);
                                    return Mono.error(new ApiException("Error from Keycloak: " + errorBody, "O_REGISTRATION_ERROR"));
                                }))
                .bodyToMono(AccessTokenResponse.class)
                .map(accessTokenResponse -> AuthResponseDto.builder()
                        .accessToken(accessTokenResponse.getToken())
                        .expiresIn(accessTokenResponse.getExpiresIn())
                        .refreshToken(accessTokenResponse.getRefreshToken())
                        .tokenType(accessTokenResponse.getTokenType())
                        .build());
    }
}