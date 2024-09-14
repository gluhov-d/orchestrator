package com.github.gluhov.orchestrator.security;

import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.AuthResponseDto;
import com.github.gluhov.orchestrator.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest
@ComponentScan("com.github.gluhov")
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties
@TestPropertySource(locations = {"classpath:application-test.yaml"})
/*@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/test",
        "spring.security.oauth2.client.registration.keycloak.client-id=test-client"
})*/
class SecurityServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private SecurityService securityService;

    @Test
    public void testAuthenticateSuccess() {
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
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
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
    public void testAuthenticateError() {
        AuthRequestDto authRequestDto = AuthRequestDto.builder()
                .username("test2@ya.ru")
                .password("password").build();

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(BodyInserters.FormInserter.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new ApiException("Error", "O_REGISTRATION_ERROR")));

        Mono<AuthResponseDto> result = securityService.authenticate(authRequestDto);

        StepVerifier.create(result)
                .expectError(ApiException.class)
                .verify();
    }
}