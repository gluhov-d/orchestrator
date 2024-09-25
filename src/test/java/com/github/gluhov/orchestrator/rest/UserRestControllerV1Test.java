package com.github.gluhov.orchestrator.rest;

import com.github.gluhov.orchestrator.dto.UserInfoDto;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class UserRestControllerV1Test {
    @InjectMocks
    private UserRestControllerV1 userRestControllerV1;
    @Mock
    private UserService userService;

    @Test
    @DisplayName("Test get user info with valid token")
    public void testGetInfo() {
        UserInfoDto userInfo = UserInfoDto.builder()
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .username("john.doe")
                .build();

        when(userService.getInfo(anyString())).thenReturn(Mono.just(userInfo));

        Mono<ResponseEntity<UserInfoDto>> result = (Mono<ResponseEntity<UserInfoDto>>) userRestControllerV1.getInfo("Bearer accessToken");
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertNotNull(r);
                    UserInfoDto userInfoDto = r.getBody();
                    assertNotNull(userInfoDto);
                    assertEquals("user@test.com", userInfoDto.getEmail());
                    assertEquals("John", userInfoDto.getFirstName());
                    assertEquals("Doe", userInfoDto.getLastName());
                })
                .verifyComplete();
    }
}