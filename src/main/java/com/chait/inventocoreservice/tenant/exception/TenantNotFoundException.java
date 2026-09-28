package com.chait.inventocoreservice.tenant.exception;

import java.util.UUID;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(UUID id) {
        super("Tenant with id [%s] not found.".formatted(id));
    }

    public TenantNotFoundException(String tenantId) {
        super("Tenant with tenantId [%s] not found.".formatted(tenantId));
    }
}
