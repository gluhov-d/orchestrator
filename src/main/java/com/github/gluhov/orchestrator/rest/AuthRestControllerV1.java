package com.github.gluhov.orchestrator.rest;

import com.github.gluhov.dto.IndividualsDto;
import com.github.gluhov.orchestrator.dto.AuthRequestDto;
import com.github.gluhov.orchestrator.dto.RefreshTokenRequestDto;
import com.github.gluhov.orchestrator.security.SecurityService;
import com.github.gluhov.orchestrator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static com.github.gluhov.orchestrator.rest.AuthRestControllerV1.REST_URL;

@RestController
@RequestMapping(REST_URL)
@RequiredArgsConstructor
public class AuthRestControllerV1 {
    public static final String REST_URL = "/api/v1/auth";
    private final SecurityService securityService;
    private final UserService userService;

    @PostMapping("/login")
    public Mono<?> login(@RequestBody AuthRequestDto authRequestDto) {
        return securityService.authenticate(authRequestDto)
                .map(authResponseDto -> ResponseEntity.ok().body(authResponseDto));
    }

    @PostMapping("/register")
    public Mono<?> register(@RequestBody IndividualsDto individualsDto) {
        return userService.register(individualsDto)
                .map(savedIndividualDto -> ResponseEntity.ok().body(savedIndividualDto));
    }

    @PostMapping("/refresh-token")
    public Mono<?> refreshToken(@RequestBody RefreshTokenRequestDto refreshTokenRequestDto) {
        return securityService.refreshToken(refreshTokenRequestDto)
                .map(authResponseDto -> ResponseEntity.ok().body(authResponseDto));
    }
}