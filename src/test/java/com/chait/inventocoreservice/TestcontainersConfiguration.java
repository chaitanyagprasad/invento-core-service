package com.chait.inventocoreservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final int REDIS_PORT = 6379;

    @Bean
    @ServiceConnection
    LgtmStackContainer grafanaLgtmContainer() {
        return new LgtmStackContainer(DockerImageName.parse("grafana/otel-lgtm:latest"));
    }

    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
                .withDatabaseName("postgres");
    }

    @Bean
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(REDIS_PORT);
    }

    @Bean
    DynamicPropertyRegistrar containerProperties(PostgreSQLContainer postgres,
                                                 GenericContainer<?> redisContainer) {
        return registry -> {
            registry.add("app.datasource.provisioning.url", postgres::getJdbcUrl);
            registry.add("app.datasource.provisioning.username", postgres::getUsername);
            registry.add("app.datasource.provisioning.password", postgres::getPassword);

            registry.add("app.datasource.platform.url", () -> "jdbc:postgresql://%s:%d/invento_platform"
                    .formatted(postgres.getHost(), postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
            registry.add("app.datasource.platform.username", postgres::getUsername);
            registry.add("app.datasource.platform.password", postgres::getPassword);

            registry.add("spring.data.redis.host", redisContainer::getHost);
            registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
        };
    }

}
