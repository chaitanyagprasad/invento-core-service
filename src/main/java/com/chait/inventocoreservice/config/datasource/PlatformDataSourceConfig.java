package com.chait.inventocoreservice.config.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;

@AllArgsConstructor
@Configuration
@EnableConfigurationProperties(DataSourceProperties.PlatformDataSourceProperties.class)
public class PlatformDataSourceConfig {
    private final DataSourceProperties.PlatformDataSourceProperties props;

    @Primary
    @Bean(name = "platformDataSource", destroyMethod = "close")
    public HikariDataSource platformDataSource(
            @Qualifier("provisioningFlyway") Flyway provisioningFlyway
    ) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(props.url());
        config.setUsername(props.username());
        config.setPassword(props.password());
        config.setDriverClassName("org.postgresql.Driver");

        // HikariCP tuning from properties
        DataSourceProperties.HikariProperties hikari = props.hikari();
        config.setPoolName(hikari.poolName());
        config.setMaximumPoolSize(hikari.maximumPoolSize());
        config.setMinimumIdle(hikari.minimumIdle());
        config.setConnectionTimeout(hikari.connectionTimeout());
        config.setIdleTimeout(hikari.idleTimeout());
        config.setMaxLifetime(hikari.maxLifetime());
        config.setKeepaliveTime(hikari.keepaliveTime());

        // Postgres-specific: set search_path so queries don't need
        // schema-qualified names.
        config.setConnectionInitSql("SET search_path TO platform");

        return new HikariDataSource(config);
    }

    @Bean(name = "platformFlyway")
    @DependsOn("provisioningFlyway")
    public Flyway platformFlyway(
            @Qualifier("platformDataSource")
            DataSource platformDataSource) {
        final Flyway flyway = Flyway.configure()
                .dataSource(platformDataSource)
                .locations("classpath:db/migration")
                .schemas("platform")
                .defaultSchema("platform")
                .table("flyway_schema_history")
                .baselineOnMigrate(false)
                .outOfOrder(false)
                .validateOnMigrate(true)
                .placeholderReplacement(false)
                .load();

        flyway.migrate();

        return flyway;
    }
}
