package com.github.gluhov.orchestrator.service;

import com.github.gluhov.dto.IndividualsDto;
import com.github.gluhov.orchestrator.exception.ApiException;
import com.github.gluhov.orchestrator.exception.AuthException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static com.github.gluhov.orchestrator.service.IndividualsData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private WebClient webClient;
    @Mock
    private Response response;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Test get user info functionality then success response")
    void givenIndividualId_whenGetById_thenSuccessResponse() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(IndividualsDto.class)).thenReturn(Mono.just(individualsDto));

        Mono<IndividualsDto> result = userService.getInfo("Bearer token", USER_ID);

        StepVerifier.create(result)
                .assertNext(individualDto -> {
                    assertNotNull(individualDto);
                    assertEquals("test@example.com", individualDto.getEmail());
                    assertEquals("1234-553-222", individualDto.getPhoneNumber());
                    assertEquals("479-80-111", individualDto.getPassportNumber());
                    assertEquals(USER_ID, individualDto.getUserId());
                }).verifyComplete();
    }

    @Test
    @DisplayName("Test get info functionality then error response")
    void givenIndividualId_whenGetById_thenErrorResponse() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(new ApiException("Error from Keycloak: ", "O_GET_INFO_ERROR")));
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        StepVerifier.create(userService.getInfo("Bearer token", UUID.randomUUID()))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof ApiException);
                    assertEquals("Error from Keycloak: ", error.getMessage());
                    assertEquals("O_GET_INFO_ERROR", ((ApiException) error).getErrorCode());
                }).verify();
    }

    @Test
    @DisplayName("Test register individual with correct credentials then success response")
    void givenIndividualsDto_whenRegister_thenSuccessResponse() {
        when(realmResource.users()).thenReturn(usersResource);

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getHeaderString("Location")).thenReturn("/users/12345");
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(IndividualsDto.class)).thenReturn(Mono.just(individualsDto));

        Mono<IndividualsDto> result = userService.register(individualsDto);

        StepVerifier.create(result)
                .assertNext(individualDto -> {
                                assertNotNull(individualDto);
                                assertEquals("test@example.com", individualDto.getEmail());
                                assertEquals("1234-553-222", individualDto.getPhoneNumber());
                                assertEquals("479-80-111", individualDto.getPassportNumber());
                                assertEquals(INDIVIDUAL_ID, individualDto.getId());
                            }).verifyComplete();

        verify(usersResource, times(1)).create(any(UserRepresentation.class));
        verify(webClient, times(1)).post();
    }

    @Test
    @DisplayName("Test register individual functionality then error response")
    void givenIndividualDto_whenRegister_thenErrorResponse() {
        when(realmResource.users()).thenReturn(usersResource);

        when(realmResource.users().create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(400);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(IndividualsDto.class)).thenReturn(Mono.just(individualsDto));

        Mono<IndividualsDto> result = userService.register(individualsDto);
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AuthException);
                    assertEquals("Failed to register user", error.getMessage());
                    assertEquals("O_REGISTER_USER_ERROR", ((ApiException) error).getErrorCode());
                }).verify();
    }

    @Test
    @DisplayName("Test register individual functionality with correct credentials but no location header in response")
    void registerMissingLocationHeader() {
        when(realmResource.users()).thenReturn(usersResource);

        when(realmResource.users().create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getHeaderString("Location")).thenReturn(null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(IndividualsDto.class)).thenReturn(Mono.just(individualsDto));

        StepVerifier.create(userService.register(individualsDto))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AuthException);
                    assertEquals("Failed to register user", error.getMessage());
                    assertEquals("O_REGISTER_USER_ERROR", ((AuthException) error).getErrorCode());
                }).verify();
    }
}