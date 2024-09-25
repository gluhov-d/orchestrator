package com.github.gluhov.orchestrator.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RealmResourceConfig {
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;
    @Value("${kc.realm}")
    private String realm;
    @Value("${kc.base-url}")
    private String baseUrl;
    @Value("${kc.user}")
    private String user;
    @Value("${kc.password}")
    private String password;

    @Bean
    Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(baseUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .username(user)
                .password(password)
                .build();
    }

    @Bean
    RealmResource realResource(Keycloak keycloak){
        return keycloak.realm(realm);
    }
}
