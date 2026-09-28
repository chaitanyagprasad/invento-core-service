package com.chait.inventocoreservice.tenant.command;

import jakarta.validation.constraints.NotNull;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record DisableTenantCommand(
        @TargetAggregateIdentifier
        @NotNull
        String id
) {
}
