package com.github.gluhov.orchestrator.security;

import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.UserInfoDto;
import com.github.gluhov.orchestrator.exception.ApiException;
import com.github.gluhov.orchestrator.service.UserService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
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

import static org.junit.Assert.*;
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
    void testGetInfoSuccess() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("test@example.com");
        userInfo.setGivenName("John");
        userInfo.setFamilyName("Doe");
        userInfo.setName("johndoe");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(any(String.class), any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserInfo.class)).thenReturn(Mono.just(userInfo));
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserInfo.class)).thenReturn(Mono.just(userInfo));

        Mono<UserInfoDto> result = userService.getInfo("Bearer token");

        UserInfoDto dto = result.block();
        Assertions.assertNotNull(dto);
        Assertions.assertEquals("test@example.com", dto.getEmail());
        Assertions.assertEquals("John", dto.getFirstName());
        Assertions.assertEquals("Doe", dto.getLastName());
        Assertions.assertEquals("johndoe", dto.getUsername());
    }

    @Test
    void testRegisterSuccess() {
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
    void testRegisterFailure() {
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
                .email("test@example.com").build();

        when(realmResource.users()).thenReturn(usersResource);

        when(realmResource.users().create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(400);

        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.register(authRequestDto).block();
        });

        Assertions.assertEquals("O_REGISTER_USER_ERROR", exception.getErrorCode());
    }

    @Test
    void testRegisterMissingLocationHeader() {
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
                .email("test@example.com").build();

        when(realmResource.users()).thenReturn(usersResource);

        when(realmResource.users().create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getHeaderString("Location")).thenReturn(null);

        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.register(authRequestDto).block();
        });

        Assertions.assertEquals("O_REGISTER_USER_ERROR", exception.getErrorCode());
    }
}
