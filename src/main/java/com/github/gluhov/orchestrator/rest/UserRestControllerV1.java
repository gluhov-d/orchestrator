package com.github.gluhov.orchestrator.rest;

import com.github.gluhov.orchestrator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.github.gluhov.orchestrator.rest.UserRestControllerV1.REST_URL;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = REST_URL)
public class UserRestControllerV1 {
    public static final String REST_URL = "/api/v1";
    private final UserService userService;

    @GetMapping(value = "/{id}/profile")
    public Mono<?> getInfo(@RequestHeader("Authorization") String authorizationHeader, @PathVariable UUID id) {
        return userService.getInfo(authorizationHeader, id)
                .map(individualsDto -> ResponseEntity.ok().body(individualsDto));
    }
}