package com.chait.inventocoreservice.tenant.projection;

import com.chait.inventocore.tenant.TenantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_view", schema = "platform")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantView {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 32)
    private String tenantId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "platform.tenant_status")
    private TenantStatus status;

    @Column(name = "db_name", length = 63)
    private String dbName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void activate() {
        this.status    = TenantStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.status    = TenantStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public void setDbName(String dbName) {
        this.dbName    = dbName;
        this.updatedAt = Instant.now();
    }
}
