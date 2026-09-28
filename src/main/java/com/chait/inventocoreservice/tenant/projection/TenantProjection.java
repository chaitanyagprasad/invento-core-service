package com.chait.inventocoreservice.tenant.projection;

import com.chait.inventocore.tenant.TenantStatus;
import com.chait.inventocoreservice.config.cache.TenantCacheConfig;
import com.chait.inventocoreservice.tenant.event.TenantActivatedEvent;
import com.chait.inventocoreservice.tenant.event.TenantCreatedEvent;
import com.chait.inventocoreservice.tenant.event.TenantDbNameSetEvent;
import com.chait.inventocoreservice.tenant.event.TenantDisabledEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@Slf4j
@AllArgsConstructor
public class TenantProjection {
    private final TenantViewRepository repository;

    @EventHandler
    public void on(TenantCreatedEvent event) {
        log.debug("Projecting TenantCreatedEvent for tenant [{}]",
                event.tenantId().value());

        TenantView view = TenantView.builder()
                .id(event.id())
                .tenantId(event.tenantId().value())
                .displayName(event.displayName())
                .status(TenantStatus.PROVISIONING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        repository.save(view);

        log.info("Tenant [{}] created in read model with id [{}]",
                event.tenantId().value(), event.id());
    }

    @CacheEvict(cacheNames = TenantCacheConfig.CACHE_NAME, key = "#event.id()")
    @EventHandler
    public void on(TenantActivatedEvent event) {
        log.debug("Projecting TenantActivatedEvent for id [{}]", event.id());

        repository.findById(event.id()).ifPresentOrElse(
                view -> {
                    view.activate();
                    repository.save(view);
                    log.info("Tenant [{}] activated in read model.", event.id());
                },
                () -> log.warn("TenantActivatedEvent received for unknown id [{}]" +
                        " — read model may be inconsistent.", event.id())
        );
    }

    @CacheEvict(cacheNames = TenantCacheConfig.CACHE_NAME, key = "#event.id()")
    @EventHandler
    public void on(TenantDisabledEvent event) {
        log.debug("Projecting TenantDisabledEvent for id [{}]", event.id());

        repository.findById(event.id()).ifPresentOrElse(
                view -> {
                    view.disable();
                    repository.save(view);
                    log.info("Tenant [{}] disabled in read model.", event.id());
                },
                () -> log.warn("TenantDisabledEvent received for unknown id [{}]" +
                        " — read model may be inconsistent.", event.id())
        );
    }

    @CacheEvict(cacheNames = TenantCacheConfig.CACHE_NAME, key = "#event.id()")
    @EventHandler
    public void on(TenantDbNameSetEvent event) {
        log.debug("Projecting TenantDbNameSetEvent for id [{}]", event.id());

        repository.findById(event.id()).ifPresentOrElse(
                view -> {
                    view.setDbName(event.dbName());
                    repository.save(view);
                    log.info("Tenant [{}] db_name set to [{}] in read model.",
                            event.id(), event.dbName());
                },
                () -> log.warn("TenantDbNameSetEvent received for unknown id [{}]" +
                        " — read model may be inconsistent.", event.id())
        );
    }
}
