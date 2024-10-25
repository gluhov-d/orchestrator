package com.github.gluhov.orchestrator.service;

import com.github.gluhov.dto.IndividualsDto;
import com.github.gluhov.orchestrator.exception.ApiException;
import com.github.gluhov.orchestrator.exception.AuthException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final RealmResource realmResource;
    private final WebClient webClient;
    @Value("${ams.base-url}")
    private String amsUri;

    public Mono<IndividualsDto> register(IndividualsDto individualsDto) {
        log.debug("try to register {}", individualsDto);
        UserRepresentation userRepresentation = getUser(individualsDto);
        return registerAMS(individualsDto).flatMap(savedIndividualDto -> {
                    String locationHeader;
                    try (Response response = realmResource.users().create(userRepresentation)){
                        if (response.getStatus() != 201) {
                            return Mono.error(new AuthException("Failed to create user", "O_REGISTER_USER_ERROR"));
                        }
                        locationHeader = response.getHeaderString("Location");
                        if (locationHeader == null || locationHeader.isEmpty()) {
                            return Mono.error(new AuthException("Missing location header after user registration", "O_REGISTER_USER_ERROR"));
                        }

                    } catch (Exception e) {
                        log.error("Error while registering new user: {}", e.getMessage());
                        return Mono.error(new ApiException("Error while registering user", "O_REGISTER_ERROR"));
                    }

                    return Mono.just(savedIndividualDto);
                })
                .doOnSuccess(userId -> log.info("Registration success for user: {}", userId))
                .onErrorResume(e -> {
                    log.error("Failed to register user {}, {}", individualsDto, e);
                    return Mono.error(new AuthException("Failed to register user", "O_REGISTER_USER_ERROR"));
                });
    }

    private Mono<IndividualsDto> registerAMS(IndividualsDto individualsDto) {
        return webClient.post()
                .uri(amsUri + "/api/v1/individuals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(individualsDto)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error from AMS: {}", errorBody);
                                    return Mono.error(new ApiException("Error from AMS: " + errorBody, "O_REGISTER_ERROR"));
                                }))
                .bodyToMono(IndividualsDto.class);
    }

    public Mono<IndividualsDto> getInfo(String authorizationHeader, UUID id) {
        return webClient.get()
                .uri(amsUri + "/api/v1/individuals/" + id + "/details")
                .header("Authorization", authorizationHeader)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error from AMS: {}", errorBody);
                                    return Mono.error(new ApiException("Error from AMS: " + errorBody, "O_GET_INFO_ERROR"));
                                }))
                .bodyToMono(IndividualsDto.class);
    }

    private UserRepresentation getUser(IndividualsDto registrationRequest) {
        UserRepresentation user = new UserRepresentation();
        user.setEmail(registrationRequest.getEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setFirstName(registrationRequest.getUser().getFirstName());
        user.setLastName(registrationRequest.getUser().getLastName());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(registrationRequest.getPassword());
        user.setCredentials(Collections.singletonList(credential));
        return user;
    }
}