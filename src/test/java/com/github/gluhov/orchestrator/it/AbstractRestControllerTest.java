package com.github.gluhov.orchestrator.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;

import java.util.function.Supplier;

public class AbstractRestControllerTest {
    @Container
    static KeycloakContainer KEYCLOAK_CONTAINER;

    static {
        KEYCLOAK_CONTAINER = new KeycloakContainer()
                .withCopyFileToContainer(MountableFile.forClasspathResource("keycloak/imports/realm-config.json"), "/opt/keycloak/data/import/realm-config.json")
                .withEnv("JAVA_OPTS", "-Dkeycloak.profile.feature.upload_scripts=enabled -Dkeycloak.migration.strategy=OVERWRITE_EXISTING -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=/opt/keycloak/data/import/realm-config.json");
        KEYCLOAK_CONTAINER.start();
    }

    @DynamicPropertySource
    public static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        final Supplier<Object> baseUrl = () -> ("http://localhost:" + KEYCLOAK_CONTAINER.getMappedPort(8080));
        registry.add("BASE_URL", baseUrl);
    }
}