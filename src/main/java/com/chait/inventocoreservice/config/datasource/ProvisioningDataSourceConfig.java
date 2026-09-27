package com.chait.inventocoreservice.config.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@AllArgsConstructor
@Configuration
@EnableConfigurationProperties(DataSourceProperties.ProvisioningDataSourceProperties.class)
public class ProvisioningDataSourceConfig {

    private final DataSourceProperties.ProvisioningDataSourceProperties props;

    @Bean(name = "provisioningDataSource", destroyMethod = "close")
    public HikariDataSource provisioningDataSource() {
        final HikariConfig config = new HikariConfig();

        config.setJdbcUrl(props.url());
        config.setUsername(props.username());
        config.setPassword(props.password());
        config.setDriverClassName("org.postgresql.Driver");

        // Minimal pool — provisioning runs once at startup then is idle.
        config.setPoolName("invento-provisioning-pool");
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(60_000);    // short — release quickly after provisioning
        config.setMaxLifetime(120_000);

        return new HikariDataSource(config);
    }

    @Bean(name = "provisioningFlyway")
    public Flyway provisioningFlyway(
            @Qualifier("provisioningDataSource")
            DataSource provisioningDataSource) {

        final Flyway flywayProvisioningHistory = Flyway.configure()
                .dataSource(provisioningDataSource)
                .locations(
                        "classpath:db/provisioning",
                        "classpath:com/chait/inventocoreservice/db/provisioning"
                )
                .table("flyway_provisioning_history")
                .outOfOrder(false)
                .validateOnMigrate(true)
                .placeholderReplacement(false)
                .mixed(true)
                .load();

        flywayProvisioningHistory.migrate();

        return flywayProvisioningHistory;
    }

}
