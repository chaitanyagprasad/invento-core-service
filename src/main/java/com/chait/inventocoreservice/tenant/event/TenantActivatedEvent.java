package com.chait.inventocoreservice.tenant.event;

import java.util.UUID;

/**
 * Published when a tenant transitions from PROVISIONING to ACTIVE.
 */
public record TenantActivatedEvent(UUID id) {}
