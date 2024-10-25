package com.github.gluhov.orchestrator.rest;

import com.github.gluhov.dto.IndividualsDto;
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

import java.util.UUID;

import static com.github.gluhov.orchestrator.service.IndividualsData.individualsDto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class UserRestControllerV1Test {
    @InjectMocks
    private UserRestControllerV1 userRestControllerV1;
    @Mock
    private UserService userService;

    @Test
    @DisplayName("Test get individual info functionality then success response")
    public void givenIndividualId_whenGetInfo_thenSuccessResponse() {
        when(userService.getInfo(anyString(), any())).thenReturn(Mono.just(individualsDto));

        Mono<ResponseEntity<IndividualsDto>> result = (Mono<ResponseEntity<IndividualsDto>>) userRestControllerV1.getInfo("Bearer accessToken", UUID.randomUUID());
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertNotNull(r);
                    IndividualsDto individualDto = r.getBody();
                    assertNotNull(individualDto);
                    assertEquals("test@example.com", individualsDto.getEmail());
                    assertEquals("Jon", individualsDto.getUser().getFirstName());
                    assertEquals("Will", individualsDto.getUser().getLastName());
                })
                .verifyComplete();
    }
}