package com.chait.inventocoreservice.tenant.query;

import com.chait.inventocore.tenant.TenantStatus;
import com.chait.inventocoreservice.config.cache.TenantCacheConfig;
import com.chait.inventocoreservice.tenant.projection.TenantView;
import com.chait.inventocoreservice.tenant.projection.TenantViewRepository;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class TenantQueryHandler {

    private final TenantViewRepository repository;

    @Cacheable(
            cacheNames = TenantCacheConfig.CACHE_NAME,
            key         = "#tenantId",
            unless      = "#result == null"
    )
    public Optional<TenantView> findByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public Optional<TenantView> findById(UUID id) {
        return repository.findById(id);
    }

    public Page<TenantView> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public List<TenantView> findByStatus(TenantStatus status) {
        return repository.findByStatus(status);
    }

    public boolean existsByTenantId(String tenantId) {
        return repository.existsByTenantId(tenantId);
    }
}
