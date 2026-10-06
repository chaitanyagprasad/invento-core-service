package com.chait.inventocoreservice.tenant.service;

import com.chait.inventocore.tenant.TenantId;
import com.chait.inventocoreservice.tenant.command.ActivateTenantCommand;
import com.chait.inventocoreservice.tenant.command.CreateTenantCommand;
import com.chait.inventocoreservice.tenant.command.DisableTenantCommand;
import com.chait.inventocoreservice.tenant.command.SetTenantDbNameCommand;
import com.chait.inventocoreservice.tenant.exception.TenantNotFoundException;
import com.chait.inventocoreservice.tenant.projection.TenantView;
import com.chait.inventocoreservice.tenant.query.TenantQueryHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class TenantCommandService {

    private final CommandGateway commandGateway;
    private final TenantQueryHandler queryHandler;

    public UUID createTenant(final String tenantId, final String displayName) {
        if (queryHandler.existsByTenantId(tenantId)) {
            throw new IllegalArgumentException(
                    "Tenant with tenantId [%s] already exists.".formatted(tenantId));
        }

        final UUID id = UUID.randomUUID();

        log.info("Creating tenant [{}] with id [{}]", tenantId, id);

        commandGateway.sendAndWait(new CreateTenantCommand(
                id.toString(),
                new TenantId(tenantId),
                displayName
        ));

        log.info("Tenant [{}] created successfully.", tenantId);
        return id;
    }

    public void activateTenant(final String tenantId) {
        final TenantView tenant = this.getTenantByTenantId(tenantId);

        log.info("Activating tenant [{}]", tenantId);

        commandGateway.sendAndWait(new ActivateTenantCommand(tenant.getId().toString()));

        log.info("Tenant [{}] activated successfully.", tenantId);
    }

    public void disableTenant(final String tenantId) {
        final TenantView tenant = this.getTenantByTenantId(tenantId);

        log.info("Disabling tenant [{}]", tenantId);

        commandGateway.sendAndWait(new DisableTenantCommand(tenant.getId().toString()));

        log.info("Tenant [{}] disabled successfully.", tenant);
    }

    public void setTenantDbName(final String tenantId, String dbName) {
        final TenantView tenant = this.getTenantByTenantId(tenantId);

        log.info("Setting db_name [{}] for tenant [{}]", dbName, tenantId);

        commandGateway.sendAndWait(new SetTenantDbNameCommand(
                tenant.getId().toString(), dbName));

        log.info("Tenant [{}] db_name set to [{}] successfully.", tenantId, dbName);
    }

    private void requireTenantExists(String tenantId) {
        if(queryHandler.existsByTenantId(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }

    private TenantView getTenantByTenantId(final String tenantId) {
        return queryHandler.findByTenantId(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }
}
