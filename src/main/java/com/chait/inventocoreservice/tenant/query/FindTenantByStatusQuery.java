package com.chait.inventocoreservice.tenant.query;

import com.chait.inventocore.tenant.TenantStatus;

public record FindTenantByStatusQuery(TenantStatus status) {}
