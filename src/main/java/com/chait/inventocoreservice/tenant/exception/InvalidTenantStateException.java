package com.chait.inventocoreservice.tenant.exception;

import com.chait.inventocore.tenant.TenantStatus;

public class InvalidTenantStateException extends RuntimeException {

    private final TenantStatus currentStatus;
    private final TenantStatus expectedStatus;

    public InvalidTenantStateException(
            String aggregateId,
            TenantStatus currentStatus,
            TenantStatus expectedStatus) {

        super("Tenant [%s] is in status [%s] but [%s] is required for this operation."
                .formatted(aggregateId, currentStatus, expectedStatus));

        this.currentStatus = currentStatus;
        this.expectedStatus = expectedStatus;
    }

    public TenantStatus currentStatus() {
        return currentStatus;
    }

    public TenantStatus expectedStatus() {
        return expectedStatus;
    }
}
