package com.github.gluhov.orchestrator.rest;

import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.AuthResponseDto;
import com.github.gluhov.orchestrator.dto.RefreshTokenRequestDto;
import com.github.gluhov.orchestrator.security.SecurityService;
import com.github.gluhov.orchestrator.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class AuthRestControllerV1Test {
    @InjectMocks
    private AuthRestControllerV1 authRestControllerV1;
    @Mock
    private SecurityService securityService;
    @Mock
    private UserService userService;

    @Test
    @DisplayName("Test login with valid credentials")
    public void testLogin() {
        AuthRequestDto authRequest = AuthRequestDto.builder()
                .email("user@test.com")
                .password("password")
                .build();
        AuthResponseDto authResponse = AuthResponseDto.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .expiresIn(3600)
                .tokenType("bearer")
                .build();

        when(securityService.authenticate(any(AuthRequestDto.class))).thenReturn(Mono.just(authResponse));

        Mono<ResponseEntity<AuthResponseDto>> result = (Mono<ResponseEntity<AuthResponseDto>>) authRestControllerV1.login(authRequest);
        StepVerifier.create(result)
                        .assertNext(r -> {
                            assertNotNull(r);
                            AuthResponseDto authResponseDto = r.getBody();
                            assertEquals(authResponseDto.getAccessToken(), "accessToken");
                            assertEquals(authResponseDto.getRefreshToken(), "refreshToken");
                        }).verifyComplete();
    }

    @Test
    @DisplayName("Test register with valid credentials")
    public void testRegister() {
        AuthRequestDto registerRequest = AuthRequestDto.builder()
                .email("user@test.com")
                .password("password")
                .build();

        when(userService.register(any(AuthRequestDto.class))).thenReturn(Mono.just("12345"));

        Mono<ResponseEntity<Void>> result = authRestControllerV1.register(registerRequest);
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertNotNull(r);
                    assertEquals("/users/12345", r.getHeaders().getLocation().toString());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Test refresh token with valid token")
    public void testRefreshToken() {
        RefreshTokenRequestDto refreshTokenRequest = RefreshTokenRequestDto.builder()
                .refreshToken("refreshToken")
                .build();
        AuthResponseDto authResponse = AuthResponseDto.builder()
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .expiresIn(3600)
                .tokenType("bearer")
                .build();

        when(securityService.refreshToken(any(RefreshTokenRequestDto.class))).thenReturn(Mono.just(authResponse));

        Mono<ResponseEntity<AuthResponseDto>> result = (Mono<ResponseEntity<AuthResponseDto>>) authRestControllerV1.refreshToken(refreshTokenRequest);
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertNotNull(r);
                    AuthResponseDto authResponseDto = r.getBody();
                    assertNotNull(authResponseDto);
                    assertEquals("newAccessToken", authResponseDto.getAccessToken());
                    assertEquals("newRefreshToken", authResponseDto.getRefreshToken());
                })
                .verifyComplete();
    }
}