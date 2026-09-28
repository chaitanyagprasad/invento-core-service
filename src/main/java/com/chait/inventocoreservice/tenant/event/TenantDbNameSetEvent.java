package com.chait.inventocoreservice.tenant.event;

import java.util.UUID;

/**
 * Published when a tenant's database name is set during provisioning.
 */
public record TenantDbNameSetEvent(
        UUID id,
        String dbName
) {}