package com.chait.inventocoreservice.tenant.service;

import com.chait.inventocore.tenant.TenantId;
import com.chait.inventocoreservice.tenant.command.ActivateTenantCommand;
import com.chait.inventocoreservice.tenant.command.CreateTenantCommand;
import com.chait.inventocoreservice.tenant.command.DisableTenantCommand;
import com.chait.inventocoreservice.tenant.command.SetTenantDbNameCommand;
import com.chait.inventocoreservice.tenant.exception.TenantNotFoundException;
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

    public UUID createTenant(String tenantId, String displayName) {
        if (queryHandler.existsByTenantId(tenantId)) {
            throw new IllegalArgumentException(
                    "Tenant with tenantId [%s] already exists.".formatted(tenantId));
        }

        UUID id = UUID.randomUUID();

        log.info("Creating tenant [{}] with id [{}]", tenantId, id);

        commandGateway.sendAndWait(new CreateTenantCommand(
                id.toString(),
                new TenantId(tenantId),
                displayName
        ));

        log.info("Tenant [{}] created successfully.", tenantId);
        return id;
    }

    public void activateTenant(UUID id) {
        requireTenantExists(id);

        log.info("Activating tenant [{}]", id);

        commandGateway.sendAndWait(new ActivateTenantCommand(id.toString()));

        log.info("Tenant [{}] activated successfully.", id);
    }

    public void disableTenant(UUID id) {
        requireTenantExists(id);

        log.info("Disabling tenant [{}]", id);

        commandGateway.sendAndWait(new DisableTenantCommand(id.toString()));

        log.info("Tenant [{}] disabled successfully.", id);
    }

    public void setTenantDbName(UUID id, String dbName) {
        requireTenantExists(id);

        log.info("Setting db_name [{}] for tenant [{}]", dbName, id);

        commandGateway.sendAndWait(new SetTenantDbNameCommand(
                id.toString(), dbName));

        log.info("Tenant [{}] db_name set to [{}] successfully.", id, dbName);
    }

    private void requireTenantExists(UUID id) {
        if (queryHandler.findById(id).isEmpty()) {
            throw new TenantNotFoundException(id);
        }
    }
}
