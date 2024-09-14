package com.github.gluhov.orchestrator.rest;

import com.github.gluhov.orchestrator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static com.github.gluhov.orchestrator.rest.UserRestControllerV1.REST_URL;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = REST_URL)
public class UserRestControllerV1 {
    public static final String REST_URL = "/api/v1/profile";
    private final UserService userService;

    @GetMapping
    public Mono<?> getInfo(@RequestHeader("Authorization") String authorizationHeader) {
        return userService.getInfo(authorizationHeader)
                .map(userInfoDto -> ResponseEntity.ok().body(userInfoDto));
    }
}