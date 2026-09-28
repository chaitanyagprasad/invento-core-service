package com.chait.inventocoreservice.tenant.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record SetTenantDbNameCommand(
        @TargetAggregateIdentifier
        @NotNull
        String id,

        @NotBlank
        String dbName
) {
}
