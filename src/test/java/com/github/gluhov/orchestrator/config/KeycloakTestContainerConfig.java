package com.github.gluhov.orchestrator.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.junit.jupiter.Container;

@TestConfiguration(proxyBeanMethods = false)
public class KeycloakTestContainerConfig {
    @Container
    static KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:latest")
            .withRealmImportFile("/keycloak/imports/realm-config.json");

    static {
        keycloakContainer.start();
    }
}
