package com.chait.inventocoreservice.config.axon;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.jdbc.JdbcTokenStore;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.jdbc.JdbcEventStorageEngine;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.SimpleQueryBus;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link AxonConfig}.
 *
 * <p>Loads the full application context with the local profile to verify
 * all Axon infrastructure beans are wired correctly against a real
 * Postgres instance. Requires the database to be running locally.
 *
 * <p>Verifies:
 * <ul>
 *   <li>All infrastructure beans are present and of the correct type.</li>
 *   <li>Serializer is Jackson-based, not XStream.</li>
 *   <li>Event store is embedded JDBC-backed, not Axon Server.</li>
 *   <li>Token store is JDBC-backed.</li>
 *   <li>Command and query buses are simple in-process implementations.</li>
 *   <li>Transaction manager bridges Spring to Axon correctly.</li>
 * </ul>
 */
@SpringBootTest
@Testcontainers
class AxonConfigTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("app.datasource.provisioning.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.provisioning.username", POSTGRES::getUsername);
        registry.add("app.datasource.provisioning.password", POSTGRES::getPassword);
        registry.add("app.datasource.platform.url", AxonConfigTest::platformJdbcUrl);
        registry.add("app.datasource.platform.username", POSTGRES::getUsername);
        registry.add("app.datasource.platform.password", POSTGRES::getPassword);
        registry.add("app.datasource.platform.hikari.pool-name",
                () -> "invento-platform-test-pool");
        registry.add("app.datasource.platform.hikari.maximum-pool-size", () -> 5);
        registry.add("app.datasource.platform.hikari.minimum-idle", () -> 1);
        registry.add("app.datasource.platform.hikari.connection-timeout", () -> 30_000);
        registry.add("app.datasource.platform.hikari.idle-timeout", () -> 600_000);
        registry.add("app.datasource.platform.hikari.max-lifetime", () -> 1_800_000);
        registry.add("app.datasource.platform.hikari.keepalive-time", () -> 60_000);
    }

    private static String platformJdbcUrl() {
        return "jdbc:postgresql://%s:%d/invento_platform"
                .formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432));
    }

    @Autowired
    Serializer axonSerializer;
    @Autowired
    EventStore eventStore;
    @Autowired
    EventStorageEngine eventStorageEngine;
    @Autowired
    TokenStore tokenStore;
    @Autowired
    CommandBus commandBus;
    @Autowired
    QueryBus queryBus;
    @Autowired
    TransactionManager axonTransactionManager;

    // ------------------------------------------------------------------
    // Serializer
    // ------------------------------------------------------------------

    @Test
    void serializer_isJacksonBased() {
        assertThat(axonSerializer).isInstanceOf(JacksonSerializer.class);
    }

    @Test
    void serializer_handlesJavaTimeTypes() {
        // If JavaTimeModule is missing, serializing Instant throws.
        java.time.Instant now = java.time.Instant.now();
        byte[] serialized = axonSerializer
                .serialize(now, byte[].class)
                .getData();
        java.time.Instant deserialized = axonSerializer.deserialize(
                axonSerializer.serialize(now, byte[].class));

        assertThat(deserialized).isEqualTo(now);
    }

    @Test
    void serializer_writesTimestampsAsIso8601NotEpoch() {
        java.time.Instant now = java.time.Instant.parse("2026-01-15T10:30:00Z");
        String serialized = axonSerializer
                .serialize(now, String.class)
                .getData();

        // Must be ISO-8601 string, not a numeric epoch timestamp.
        assertThat(serialized).contains("2026-01-15");
        assertThat(serialized).doesNotMatch("^\\d+$");
    }

    // ------------------------------------------------------------------
    // Event store
    // ------------------------------------------------------------------

    @Test
    void eventStore_isEmbeddedNotAxonServer() {
        assertThat(eventStore).isInstanceOf(EmbeddedEventStore.class);
    }

    @Test
    void eventStore_storageEngine_isJdbcBacked() {
        assertThat(eventStorageEngine)
                .isInstanceOf(JdbcEventStorageEngine.class);
    }

    // ------------------------------------------------------------------
    // Token store
    // ------------------------------------------------------------------

    @Test
    void tokenStore_isJdbcBacked() {
        assertThat(tokenStore).isInstanceOf(JdbcTokenStore.class);
    }

    // ------------------------------------------------------------------
    // Command bus
    // ------------------------------------------------------------------

    @Test
    void commandBus_isSimpleInProcess() {
        assertThat(commandBus).isInstanceOf(SimpleCommandBus.class);
    }

    // ------------------------------------------------------------------
    // Query bus
    // ------------------------------------------------------------------

    @Test
    void queryBus_isSimpleInProcess() {
        assertThat(queryBus).isInstanceOf(SimpleQueryBus.class);
    }

    // ------------------------------------------------------------------
    // Transaction manager
    // ------------------------------------------------------------------

    @Test
    void transactionManager_bridgesSpringToAxon() {
        assertThat(axonTransactionManager)
                .isInstanceOf(SpringTransactionManager.class);
    }
}