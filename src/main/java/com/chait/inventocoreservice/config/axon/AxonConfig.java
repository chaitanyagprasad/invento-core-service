package com.chait.inventocoreservice.config.axon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.common.jdbc.PersistenceExceptionResolver;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.jdbc.JdbcTokenStore;
import org.axonframework.eventhandling.tokenstore.jdbc.TokenSchema;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.jdbc.EventSchema;
import org.axonframework.eventsourcing.eventstore.jdbc.JdbcEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jdbc.JdbcSQLErrorCodesResolver;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.SimpleQueryBus;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class AxonConfig {

    @Bean
    @Primary
    public Serializer axonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return JacksonSerializer.builder()
                .objectMapper(objectMapper)
                .build();
    }

    @Bean
    public PersistenceExceptionResolver persistenceExceptionResolver() {
        return new JdbcSQLErrorCodesResolver();
    }

    @Bean
    public TransactionManager axonTransactionManager(
            PlatformTransactionManager platformTransactionManager) {
        return new SpringTransactionManager(platformTransactionManager);
    }

    @Bean
    public EventStorageEngine eventStorageEngine(
            Serializer axonSerializer,
            PersistenceExceptionResolver persistenceExceptionResolver,
            @Qualifier("platformDataSource") DataSource platformDataSource,
            TransactionManager axonTransactionManager) {
        return JdbcEventStorageEngine.builder()
                .snapshotSerializer(axonSerializer)
                .eventSerializer(axonSerializer)
                .persistenceExceptionResolver(persistenceExceptionResolver)
                .connectionProvider(platformDataSource::getConnection)
                .schema(EventSchema.builder()
                        .eventTable("domain_event_entry")
                        .snapshotTable("snapshot_event_entry")
                        .build())
                .transactionManager(axonTransactionManager)
                .build();
    }

    @Bean
    public EventStore eventStore(EventStorageEngine eventStorageEngine) {
        return EmbeddedEventStore.builder()
                .storageEngine(eventStorageEngine)
                .build();
    }

    @Bean
    public TokenStore tokenStore(
            Serializer axonSerializer,
            @Qualifier("platformDataSource") DataSource platformDataSource) {
        return JdbcTokenStore.builder()
                .serializer(axonSerializer)
                .connectionProvider(platformDataSource::getConnection)
                .schema(TokenSchema.builder().build())
                .build();
    }

    @Bean
    public CommandBus commandBus(TransactionManager axonTransactionManager) {
        SimpleCommandBus commandBus = SimpleCommandBus.builder()
                .transactionManager(axonTransactionManager)
                .build();
        commandBus.registerDispatchInterceptor(new BeanValidationInterceptor<>());
        return commandBus;
    }

    @Bean
    public QueryBus queryBus() {
        return SimpleQueryBus.builder().build();
    }
}
