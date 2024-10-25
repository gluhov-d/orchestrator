package com.github.gluhov.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gluhov.dto.AddressesDto;
import com.github.gluhov.dto.IndividualsDto;
import com.github.gluhov.dto.UsersDto;

import java.util.UUID;

public class IndividualsData {
    public static final UUID INDIVIDUAL_ID = UUID.randomUUID();
    public static final UUID USER_ID = UUID.randomUUID();
    public static final UUID ADDRESS_ID = UUID.randomUUID();
    public static final UUID COUNTRY_ID = UUID.randomUUID();
    public static final AddressesDto addressDto = AddressesDto.builder()
            .state("State")
            .city("City")
            .address("Street")
            .zipCode("12345")
            .countryId(COUNTRY_ID)
            .build();
    public static final UsersDto usersDto = UsersDto.builder()
            .firstName("Jon")
            .lastName("Will")
            .addressId(ADDRESS_ID)
            .addresses(addressDto)
            .build();
    public static final IndividualsDto individualsDto = IndividualsDto.builder()
            .id(INDIVIDUAL_ID)
            .userId(USER_ID)
            .email("test@example.com")
            .password("password")
            .phoneNumber("1234-553-222")
            .passportNumber("479-80-111")
            .user(usersDto)
            .build();

    public static String jsonResponse;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonResponse = objectMapper.writeValueAsString(individualsDto);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}