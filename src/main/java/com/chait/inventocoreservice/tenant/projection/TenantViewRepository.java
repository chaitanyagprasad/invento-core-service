package com.chait.inventocoreservice.tenant.projection;

import com.chait.inventocore.tenant.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantViewRepository extends JpaRepository<TenantView, UUID> {

    Optional<TenantView> findByTenantId(String tenantId);

    List<TenantView> findByStatus(TenantStatus status);

    boolean existsByTenantId(String tenantId);

    Page<TenantView> findAll(Pageable pageable);
}
