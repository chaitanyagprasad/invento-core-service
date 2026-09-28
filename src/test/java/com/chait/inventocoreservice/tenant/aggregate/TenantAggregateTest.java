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
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantAggregate} using Axon's
 * {@link AggregateTestFixture}.
 *
 * <p>The fixture provides a given/when/then DSL:
 * <ul>
 *   <li>{@code given} — pre-existing events that reconstruct aggregate state.</li>
 *   <li>{@code when} — the command under test.</li>
 *   <li>{@code expectEvents} — events the command should publish.</li>
 *   <li>{@code expectException} — exception the command should throw.</li>
 * </ul>
 *
 * No Spring context, no database — pure in-memory aggregate testing.
 */
class TenantAggregateTest {

    private FixtureConfiguration<TenantAggregate> fixture;

    private static final String AGGREGATE_ID = UUID.randomUUID().toString();
    private static final TenantId TENANT_ID  = new TenantId("acme");
    private static final String DISPLAY_NAME = "Acme Corporation";
    private static final String DB_NAME      = "invento_acme";

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(TenantAggregate.class);
    }

    // ------------------------------------------------------------------
    // CreateTenantCommand
    // ------------------------------------------------------------------

    @Nested
    class CreateTenant {

        @Test
        void createTenant_publishesTenantCreatedEvent() {
            fixture.givenNoPriorActivity()
                    .when(new CreateTenantCommand(
                            AGGREGATE_ID, TENANT_ID, DISPLAY_NAME))
                    .expectEvents(new TenantCreatedEvent(
                            UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME))
                    .expectSuccessfulHandlerExecution();
        }

        @Test
        void createTenant_aggregateStateIsProvisioning() {
            fixture.givenNoPriorActivity()
                    .when(new CreateTenantCommand(
                            AGGREGATE_ID, TENANT_ID, DISPLAY_NAME))
                    .expectState(aggregate -> {
                        assertThat(aggregate.status()).isEqualTo(TenantStatus.PROVISIONING);
                        assertThat(aggregate.tenantId()).isEqualTo(TENANT_ID);
                        assertThat(aggregate.displayName()).isEqualTo(DISPLAY_NAME);
                        assertThat(aggregate.dbName()).isNull();
                    });
        }
    }

    // ------------------------------------------------------------------
    // SetTenantDbNameCommand
    // ------------------------------------------------------------------

    @Nested
    class SetTenantDbName {

        @Test
        void setDbName_whenProvisioning_publishesDbNameSetEvent() {
            fixture.given(new TenantCreatedEvent(
                            UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME))
                    .when(new SetTenantDbNameCommand(AGGREGATE_ID, DB_NAME))
                    .expectEvents(new TenantDbNameSetEvent(
                            UUID.fromString(AGGREGATE_ID), DB_NAME))
                    .expectSuccessfulHandlerExecution();
        }

        @Test
        void setDbName_aggregateStateHasDbName() {
            fixture.given(new TenantCreatedEvent(
                            UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME))
                    .when(new SetTenantDbNameCommand(AGGREGATE_ID, DB_NAME))
                    .expectState(aggregate ->
                            assertThat(aggregate.dbName()).isEqualTo(DB_NAME));
        }

        @Test
        void setDbName_whenActive_throwsInvalidTenantStateException() {
            fixture.given(
                            new TenantCreatedEvent(
                                    UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME),
                            new TenantActivatedEvent(UUID.fromString(AGGREGATE_ID)))
                    .when(new SetTenantDbNameCommand(AGGREGATE_ID, DB_NAME))
                    .expectException(InvalidTenantStateException.class)
                    .expectExceptionMessage(org.hamcrest.Matchers.containsString("ACTIVE"))
                    .expectExceptionMessage(org.hamcrest.Matchers.containsString("PROVISIONING"));
        }

        @Test
        void setDbName_whenDisabled_throwsInvalidTenantStateException() {
            fixture.given(
                            new TenantCreatedEvent(
                                    UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME),
                            new TenantActivatedEvent(UUID.fromString(AGGREGATE_ID)),
                            new TenantDisabledEvent(UUID.fromString(AGGREGATE_ID)))
                    .when(new SetTenantDbNameCommand(AGGREGATE_ID, DB_NAME))
                    .expectException(InvalidTenantStateException.class);
        }
    }

    // ------------------------------------------------------------------
    // ActivateTenantCommand
    // ------------------------------------------------------------------

    @Nested
    class ActivateTenant {

        @Test
        void activate_whenProvisioning_publishesActivatedEvent() {
            fixture.given(new TenantCreatedEvent(
                            UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME))
                    .when(new ActivateTenantCommand(AGGREGATE_ID))
                    .expectEvents(new TenantActivatedEvent(
                            UUID.fromString(AGGREGATE_ID)))
                    .expectSuccessfulHandlerExecution();
        }

        @Test
        void activate_aggregateStateIsActive() {
            fixture.given(new TenantCreatedEvent(
                            UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME))
                    .when(new ActivateTenantCommand(AGGREGATE_ID))
                    .expectState(aggregate ->
                            assertThat(aggregate.status()).isEqualTo(TenantStatus.ACTIVE));
        }

        @Test
        void activate_whenAlreadyActive_throwsInvalidTenantStateException() {
            fixture.given(
                            new TenantCreatedEvent(
                                    UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME),
                            new TenantActivatedEvent(UUID.fromString(AGGREGATE_ID)))
                    .when(new ActivateTenantCommand(AGGREGATE_ID))
                    .expectException(InvalidTenantStateException.class)
                    .expectExceptionMessage(
                            org.hamcrest.Matchers.containsString("ACTIVE"));
        }

        @Test
        void activate_whenDisabled_throwsInvalidTenantStateException() {
            fixture.given(
                            new TenantCreatedEvent(
                                    UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME),
                            new TenantActivatedEvent(UUID.fromString(AGGREGATE_ID)),
                            new TenantDisabledEvent(UUID.fromString(AGGREGATE_ID)))
                    .when(new ActivateTenantCommand(AGGREGATE_ID))
                    .expectException(InvalidTenantStateException.class)
                    .expectExceptionMessage(
                            org.hamcrest.Matchers.containsString("DISABLED"));
        }
    }

    // ------------------------------------------------------------------
    // DisableTenantCommand
    // ------------------------------------------------------------------

    @Nested
    class DisableTenant {

        @Test
        void disable_whenActive_publishesDisabledEvent() {
            fixture.given(
                            new TenantCreatedEvent(
                                    UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME),
                            new TenantActivatedEvent(UUID.fromString(AGGREGATE_ID)))
                    .when(new DisableTenantCommand(AGGREGATE_ID))
                    .expectEvents(new TenantDisabledEvent(
                            UUID.fromString(AGGREGATE_ID)))
                    .expectSuccessfulHandlerExecution();
        }

        @Test
        void disable_aggregateStateIsDisabled() {
            fixture.given(
                            new TenantCreatedEvent(
                                    UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME),
                            new TenantActivatedEvent(UUID.fromString(AGGREGATE_ID)))
                    .when(new DisableTenantCommand(AGGREGATE_ID))
                    .expectState(aggregate ->
                            assertThat(aggregate.status()).isEqualTo(TenantStatus.DISABLED));
        }

        @Test
        void disable_whenProvisioning_throwsInvalidTenantStateException() {
            fixture.given(new TenantCreatedEvent(
                            UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME))
                    .when(new DisableTenantCommand(AGGREGATE_ID))
                    .expectException(InvalidTenantStateException.class)
                    .expectExceptionMessage(
                            org.hamcrest.Matchers.containsString("PROVISIONING"))
                    .expectExceptionMessage(
                            org.hamcrest.Matchers.containsString("ACTIVE"));
        }

        @Test
        void disable_whenAlreadyDisabled_throwsInvalidTenantStateException() {
            fixture.given(
                            new TenantCreatedEvent(
                                    UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME),
                            new TenantActivatedEvent(UUID.fromString(AGGREGATE_ID)),
                            new TenantDisabledEvent(UUID.fromString(AGGREGATE_ID)))
                    .when(new DisableTenantCommand(AGGREGATE_ID))
                    .expectException(InvalidTenantStateException.class);
        }
    }

    // ------------------------------------------------------------------
    // Event sourcing state reconstruction
    // ------------------------------------------------------------------

    @Nested
    class EventSourcingReconstruction {

        @Test
        void fullLifecycle_stateReconstructedCorrectlyFromEvents() {
            fixture.given(
                            new TenantCreatedEvent(
                                    UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME),
                            new TenantDbNameSetEvent(UUID.fromString(AGGREGATE_ID), DB_NAME),
                            new TenantActivatedEvent(UUID.fromString(AGGREGATE_ID)))
                    .when(new DisableTenantCommand(AGGREGATE_ID))
                    .expectState(aggregate -> {
                        assertThat(aggregate.id()).isEqualTo(AGGREGATE_ID);
                        assertThat(aggregate.tenantId()).isEqualTo(TENANT_ID);
                        assertThat(aggregate.displayName()).isEqualTo(DISPLAY_NAME);
                        assertThat(aggregate.dbName()).isEqualTo(DB_NAME);
                        assertThat(aggregate.status()).isEqualTo(TenantStatus.DISABLED);
                    });
        }

        @Test
        void replay_withOnlyCreateEvent_givesProvisioningState() {
            fixture.given(new TenantCreatedEvent(
                            UUID.fromString(AGGREGATE_ID), TENANT_ID, DISPLAY_NAME))
                    .when(new ActivateTenantCommand(AGGREGATE_ID))
                    .expectState(aggregate ->
                            assertThat(aggregate.status()).isEqualTo(TenantStatus.ACTIVE));
        }
    }
}