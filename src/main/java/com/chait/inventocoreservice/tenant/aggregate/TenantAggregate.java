package com.chait.inventocoreservice.tenant.aggregate;

import com.chait.inventocore.tenant.TenantId;
import com.chait.inventocore.tenant.TenantStatus;
import com.chait.inventocoreservice.tenant.command.ActivateTenantCommand;
import com.chait.inventocoreservice.tenant.command.CreateTenantCommand;
import com.chait.inventocoreservice.tenant.command.DisableTenantCommand;
import com.chait.inventocoreservice.tenant.command.SetTenantDbNameCommand;
import com.chait.inventocoreservice.tenant.event.TenantActivatedEvent;
import com.chait.inventocoreservice.tenant.event.TenantCreatedEvent;
import com.chait.inventocoreservice.tenant.event.TenantDbNameSetEvent;
import com.chait.inventocoreservice.tenant.event.TenantDisabledEvent;
import com.chait.inventocoreservice.tenant.exception.InvalidTenantStateException;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.UUID;

@Aggregate
public class TenantAggregate {

    @AggregateIdentifier
    private String id;

    private TenantId tenantId;
    private String displayName;
    private TenantStatus status;
    private String dbName;

    String id() {
        return id;
    }

    TenantId tenantId() {
        return tenantId;
    }

    String displayName() {
        return displayName;
    }

    TenantStatus status() {
        return status;
    }

    String dbName() {
        return dbName;
    }

    /**
     * Required by Axon for event sourcing reconstruction.
     * Never called directly — Axon uses it when replaying events.
     */
    protected TenantAggregate() {}

    /**
     * Creates a new tenant in {@code PROVISIONING} state.
     *
     * <p>This is the aggregate constructor command handler. The aggregate
     * identifier is supplied by the caller (generated in
     * {@code TenantCommandService}) — never generated here.
     */
    @CommandHandler
    public TenantAggregate(CreateTenantCommand command) {
        AggregateLifecycle.apply(new TenantCreatedEvent(
                UUID.fromString(command.id()),
                command.tenantId(),
                command.displayName()
        ));
    }

    /**
     * Transitions the tenant from {@code PROVISIONING} to {@code ACTIVE}.
     *
     * @throws InvalidTenantStateException if status is not {@code PROVISIONING}
     */
    @CommandHandler
    public void handle(ActivateTenantCommand command) {
        requireStatus(TenantStatus.PROVISIONING);
        AggregateLifecycle.apply(new TenantActivatedEvent(
                UUID.fromString(command.id())
        ));
    }

    /**
     * Transitions the tenant from {@code ACTIVE} to {@code DISABLED}.
     *
     * @throws InvalidTenantStateException if status is not {@code ACTIVE}
     */
    @CommandHandler
    public void handle(DisableTenantCommand command) {
        requireStatus(TenantStatus.ACTIVE);
        AggregateLifecycle.apply(new TenantDisabledEvent(
                UUID.fromString(command.id())
        ));
    }

    /**
     * Sets the tenant's database name during provisioning.
     *
     * @throws InvalidTenantStateException if status is not {@code PROVISIONING}
     */
    @CommandHandler
    public void handle(SetTenantDbNameCommand command) {
        requireStatus(TenantStatus.PROVISIONING);
        AggregateLifecycle.apply(new TenantDbNameSetEvent(
                UUID.fromString(command.id()),
                command.dbName()
        ));
    }

    @EventSourcingHandler
    public void on(TenantCreatedEvent event) {
        this.id          = event.id().toString();
        this.tenantId    = event.tenantId();
        this.displayName = event.displayName();
        this.status      = TenantStatus.PROVISIONING;
    }

    @EventSourcingHandler
    public void on(TenantActivatedEvent event) {
        this.status = TenantStatus.ACTIVE;
    }

    @EventSourcingHandler
    public void on(TenantDisabledEvent event) {
        this.status = TenantStatus.DISABLED;
    }

    @EventSourcingHandler
    public void on(TenantDbNameSetEvent event) {
        this.dbName = event.dbName();
    }

    private void requireStatus(TenantStatus required) {
        if (this.status != required) {
            throw new InvalidTenantStateException(this.id, this.status, required);
        }
    }
}
