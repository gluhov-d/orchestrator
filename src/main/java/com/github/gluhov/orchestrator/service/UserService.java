package com.github.gluhov.orchestrator.service;

import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.UserInfoDto;
import com.github.gluhov.orchestrator.exception.ApiException;
import com.github.gluhov.orchestrator.exception.AuthException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final RealmResource realmResource;
    private final WebClient webClient;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String keycloakIssuerUri;

    public Mono<String> register(AuthRequestDto authRequestDto) {
        log.debug("try to register {}", authRequestDto);
        UserRepresentation userRepresentation = getUser(authRequestDto);

        return Mono.fromCallable(() -> {
                    String locationHeader;
                    try {
                        Response response = realmResource.users().create(userRepresentation);
                        if (response.getStatus() != 201) {
                            throw new AuthException("Failed to create user", "O_REGISTER_USER_ERROR");
                        }
                        locationHeader = response.getHeaderString("Location");
                        if (locationHeader == null || locationHeader.isEmpty()) {
                            throw new AuthException("Missing location header after user registration", "O_REGISTER_USER_ERROR");
                        }

                    } catch (Exception e) {
                        log.error("Error while registering new user: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }

                    return locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
                })
                .doOnSuccess(userId -> log.info("Registration success for user: {}", userId))
                .onErrorResume(e -> {
                    log.error("Failed to register user {}", authRequestDto, e);
                    return Mono.error(new AuthException("Failed to register user", "O_REGISTER_USER_ERROR"));
                });
    }

    public Mono<UserInfoDto> getInfo(String authorizationHeader) {
        return webClient.get()
                .uri(keycloakIssuerUri + "/protocol/openid-connect/userinfo")
                .header("Authorization", authorizationHeader)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error from Keycloak: {}", errorBody);
                                    return Mono.error(new ApiException("Error from Keycloak: " + errorBody, "O_GET_INFO_ERROR"));
                                }))
                .bodyToMono(UserInfo.class)
                .map(userInfo -> UserInfoDto.builder()
                        .email(userInfo.getEmail())
                        .firstName(userInfo.getGivenName())
                        .lastName(userInfo.getFamilyName())
                        .username(userInfo.getName())
                        .build());
    }

    private UserRepresentation getUser(AuthRequestDto registrationRequest) {
        UserRepresentation user = new UserRepresentation();
        user.setEmail(registrationRequest.getEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setFirstName(registrationRequest.getFirstName());
        user.setLastName(registrationRequest.getLastName());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(registrationRequest.getPassword());
        user.setCredentials(Collections.singletonList(credential));
        return user;
    }
}