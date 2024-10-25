package com.github.gluhov.orchestrator.rest;

import com.github.gluhov.dto.IndividualsDto;
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

import static com.github.gluhov.orchestrator.service.IndividualsData.INDIVIDUAL_ID;
import static com.github.gluhov.orchestrator.service.IndividualsData.individualsDto;
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
    @DisplayName("Test login functionality then success response")
    public void givenAuthRequest_whenLogin_thenSuccessResponse() {
        AuthRequestDto authRequest = AuthRequestDto.builder()
                .username("user@test.com")
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
    @DisplayName("Test register individual with valid credentials then success response")
    public void givenIndividualDto_whenRegister_thenSuccessResponse() {

        when(userService.register(any(IndividualsDto.class))).thenReturn(Mono.just(individualsDto));

        Mono<ResponseEntity<IndividualsDto>> result = (Mono<ResponseEntity<IndividualsDto>>) authRestControllerV1.register(individualsDto);
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertNotNull(r);
                    IndividualsDto individualDto = r.getBody();
                    assertEquals("test@example.com", individualDto.getEmail());
                    assertEquals("1234-553-222", individualDto.getPhoneNumber());
                    assertEquals("479-80-111", individualDto.getPassportNumber());
                    assertEquals(INDIVIDUAL_ID, individualDto.getId());
                }).verifyComplete();
    }

    @Test
    @DisplayName("Test refresh token functionality then success response")
    public void givenRefreshTokenRequest_whenRefreshToken_thenSuccessResponse() {
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