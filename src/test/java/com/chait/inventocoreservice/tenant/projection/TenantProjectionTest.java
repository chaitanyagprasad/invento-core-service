package com.chait.inventocoreservice.tenant.projection;

import com.chait.inventocore.tenant.TenantId;
import com.chait.inventocore.tenant.TenantStatus;
import com.chait.inventocoreservice.tenant.event.TenantActivatedEvent;
import com.chait.inventocoreservice.tenant.event.TenantCreatedEvent;
import com.chait.inventocoreservice.tenant.event.TenantDbNameSetEvent;
import com.chait.inventocoreservice.tenant.event.TenantDisabledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantProjectionTest {

    @Mock
    private TenantViewRepository repository;

    private TenantProjection projection;

    private static final UUID ID           = UUID.randomUUID();
    private static final TenantId TENANT_ID    = new TenantId("acme");
    private static final String    DISPLAY_NAME = "Acme Corporation";
    private static final String    DB_NAME      = "invento_acme";

    @BeforeEach
    void setUp() {
        projection = new TenantProjection(repository);
    }

    @Nested
    class OnTenantCreated {

        @Test
        void savesNewTenantViewWithProvisioningStatus() {
            ArgumentCaptor<TenantView> captor =
                    ArgumentCaptor.forClass(TenantView.class);

            projection.on(new TenantCreatedEvent(ID, TENANT_ID, DISPLAY_NAME));

            verify(repository).save(captor.capture());
            TenantView saved = captor.getValue();

            assertThat(saved.getId()).isEqualTo(ID);
            assertThat(saved.getTenantId()).isEqualTo("acme");
            assertThat(saved.getDisplayName()).isEqualTo(DISPLAY_NAME);
            assertThat(saved.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
            assertThat(saved.getDbName()).isNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    class OnTenantActivated {

        @Test
        void updatesStatusToActive_whenTenantExists() {
            TenantView existing = existingView(TenantStatus.PROVISIONING);
            when(repository.findById(ID)).thenReturn(Optional.of(existing));

            projection.on(new TenantActivatedEvent(ID));

            verify(repository).save(existing);
            assertThat(existing.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        }

        @Test
        void doesNotSave_whenTenantNotFound() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            projection.on(new TenantActivatedEvent(ID));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class OnTenantDisabled {

        @Test
        void updatesStatusToDisabled_whenTenantExists() {
            TenantView existing = existingView(TenantStatus.ACTIVE);
            when(repository.findById(ID)).thenReturn(Optional.of(existing));

            projection.on(new TenantDisabledEvent(ID));

            verify(repository).save(existing);
            assertThat(existing.getStatus()).isEqualTo(TenantStatus.DISABLED);
        }

        @Test
        void doesNotSave_whenTenantNotFound() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            projection.on(new TenantDisabledEvent(ID));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class OnTenantDbNameSet {

        @Test
        void updatesDbName_whenTenantExists() {
            TenantView existing = existingView(TenantStatus.PROVISIONING);
            when(repository.findById(ID)).thenReturn(Optional.of(existing));

            projection.on(new TenantDbNameSetEvent(ID, DB_NAME));

            verify(repository).save(existing);
            assertThat(existing.getDbName()).isEqualTo(DB_NAME);
        }

        @Test
        void doesNotSave_whenTenantNotFound() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            projection.on(new TenantDbNameSetEvent(ID, DB_NAME));

            verify(repository, never()).save(any());
        }
    }

    private TenantView existingView(TenantStatus status) {
        return TenantView.builder()
                .id(ID)
                .tenantId(TENANT_ID.value())
                .displayName(DISPLAY_NAME)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

}