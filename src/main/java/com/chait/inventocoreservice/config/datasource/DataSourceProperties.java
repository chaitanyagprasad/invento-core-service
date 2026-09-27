package com.chait.inventocoreservice.config.datasource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataSourceProperties {

    @ConfigurationProperties("app.datasource.platform")
    public record PlatformDataSourceProperties(
            String url,
            String username,
            String password,
            HikariProperties hikari
    ) {}

    @ConfigurationProperties("app.datasource.provisioning")
    public record ProvisioningDataSourceProperties(
            String url,
            String username,
            String password
    ) {}

    public record HikariProperties(
            String poolName,
            int maximumPoolSize,
            int minimumIdle,
            long connectionTimeout,
            long idleTimeout,
            long maxLifetime,
            long keepaliveTime
    ) {}
}
