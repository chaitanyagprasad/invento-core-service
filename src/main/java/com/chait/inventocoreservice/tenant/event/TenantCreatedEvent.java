package com.chait.inventocoreservice.tenant.event;

import com.chait.inventocore.tenant.TenantId;
import java.util.UUID;

public record TenantCreatedEvent(
        UUID id,
        TenantId tenantId,
        String displayName
) {
}
