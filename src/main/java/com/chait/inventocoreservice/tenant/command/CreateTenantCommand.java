package com.chait.inventocoreservice.tenant.command;

import com.chait.inventocore.tenant.TenantId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record CreateTenantCommand(
        @TargetAggregateIdentifier
        @NotNull
        String id,
        @NotNull
        TenantId tenantId,
        @NotBlank
        String displayName
) {
}
