package com.chait.inventocoreservice.tenant.event;

import java.util.UUID;

/**
 * Published when a tenant transitions from ACTIVE to DISABLED.
 */
public record TenantDisabledEvent(UUID id) {}