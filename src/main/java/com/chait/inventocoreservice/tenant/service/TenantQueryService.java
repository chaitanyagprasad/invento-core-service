package com.chait.inventocoreservice.tenant.service;

import com.chait.inventocore.tenant.TenantStatus;
import com.chait.inventocoreservice.tenant.exception.TenantNotFoundException;
import com.chait.inventocoreservice.tenant.projection.TenantView;
import com.chait.inventocoreservice.tenant.query.TenantQueryHandler;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class TenantQueryService {

    private final TenantQueryHandler queryHandler;

    public TenantView findByTenantId(String tenantId) {
        return queryHandler.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    public TenantView findById(UUID id) {
        return queryHandler.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    public Page<TenantView> findAll(Pageable pageable) {
        return queryHandler.findAll(pageable);
    }

    public List<TenantView> findByStatus(TenantStatus status) {
        return queryHandler.findByStatus(status);
    }
}
