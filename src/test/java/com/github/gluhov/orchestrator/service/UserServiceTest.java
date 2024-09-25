package com.github.gluhov.orchestrator.service;

import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.UserInfoDto;
import com.github.gluhov.orchestrator.exception.ApiException;
import com.github.gluhov.orchestrator.exception.AuthException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Test get user info success")
    void getInfoSuccess() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("test@example.com");
        userInfo.setGivenName("John");
        userInfo.setFamilyName("Doe");
        userInfo.setName("johndoe");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserInfo.class)).thenReturn(Mono.just(userInfo));

        Mono<UserInfoDto> result = userService.getInfo("Bearer token");

        StepVerifier.create(result)
                .assertNext(userInfoDto -> {
                    assertNotNull(userInfoDto);
                    assertEquals("test@example.com", userInfoDto.getEmail());
                    assertEquals("John", userInfoDto.getFirstName());
                    assertEquals("Doe", userInfoDto.getLastName());
                    assertEquals("johndoe", userInfoDto.getUsername());
                }).verifyComplete();
    }

    @Test
    @DisplayName("Test get info with error")
    void getInfoFailure() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(new ApiException("Error from Keycloak: ", "O_GET_INFO_ERROR")));
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        StepVerifier.create(userService.getInfo("Bearer token"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof ApiException);
                    assertEquals("Error from Keycloak: ", error.getMessage());
                    assertEquals("O_GET_INFO_ERROR", ((ApiException) error).getErrorCode());
                }).verify();
    }

    @Test
    @DisplayName("Test register user with correct credentials")
    void registerSuccess() {
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
                .email("test@example.com")
                .password("password")
                .lastName("Doe")
                .firstName("John").build();

        when(realmResource.users()).thenReturn(usersResource);

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getHeaderString("Location")).thenReturn("/users/12345");
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        Mono<String> result = userService.register(authRequestDto);

        StepVerifier.create(result)
                .expectNext("12345")
                .verifyComplete();

        verify(usersResource, times(1)).create(any(UserRepresentation.class));
    }

    @Test
    @DisplayName("Test register user with wrong credentials")
    void registerFailure() {
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
                .email("test@example.com").build();

        when(realmResource.users()).thenReturn(usersResource);

        when(realmResource.users().create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(400);
        Mono<String> result = userService.register(authRequestDto);
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AuthException);
                    assertEquals("Failed to register user", error.getMessage());
                    assertEquals("O_REGISTER_USER_ERROR", ((ApiException) error).getErrorCode());
                }).verify();
    }

    @Test
    @DisplayName("Test register user with correct credentials but no location header in response")
    void registerMissingLocationHeader() {
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
                .email("test@example.com").build();

        when(realmResource.users()).thenReturn(usersResource);

        when(realmResource.users().create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getHeaderString("Location")).thenReturn(null);

        StepVerifier.create(userService.register(authRequestDto))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AuthException);
                    assertEquals("Failed to register user", error.getMessage());
                    assertEquals("O_REGISTER_USER_ERROR", ((AuthException) error).getErrorCode());
                }).verify();
    }
}