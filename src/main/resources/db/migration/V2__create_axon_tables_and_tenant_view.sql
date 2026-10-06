-- -----------------------------------------------------------------------
-- V2 : Axon Framework tables + tenant_view read model
--
-- Axon tables are created in the platform schema alongside the tenant
-- registry. All Axon auto-creation is disabled in AxonConfig — Flyway
-- owns the schema lifecycle.
--
-- tenant_view is the read model projection maintained by TenantProjection.
-- It is written only by event handlers, never by the command side.
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- Axon event store
-- Holds all domain events for every aggregate in the platform store.
-- One row per event — append only, never updated or deleted.
-- -----------------------------------------------------------------------
CREATE TABLE platform.domain_event_entry (
                                             global_index         BIGSERIAL       NOT NULL,
                                             aggregate_identifier VARCHAR(255)    NOT NULL,
                                             sequence_number      BIGINT          NOT NULL,
                                             type                 VARCHAR(255),
                                             event_identifier     VARCHAR(255)    NOT NULL UNIQUE,
                                             meta_data            BYTEA,
                                             payload              BYTEA           NOT NULL,
                                             payload_revision     VARCHAR(255),
                                             payload_type         VARCHAR(255)    NOT NULL,
                                             time_stamp           VARCHAR(255)    NOT NULL,

                                             PRIMARY KEY (global_index),
                                             CONSTRAINT uq_domain_event_aggregate_sequence
                                                 UNIQUE (aggregate_identifier, sequence_number)
);

CREATE INDEX idx_domain_event_entry_aggregate
    ON platform.domain_event_entry (aggregate_identifier, sequence_number);

CREATE INDEX idx_domain_event_entry_type
    ON platform.domain_event_entry (type);

-- -----------------------------------------------------------------------
-- Axon snapshot store
-- Holds aggregate snapshots to speed up event replay for long-lived
-- aggregates. Optional but included for completeness.
-- -----------------------------------------------------------------------
CREATE TABLE platform.snapshot_event_entry (
                                               aggregate_identifier VARCHAR(255)    NOT NULL,
                                               sequence_number      BIGINT          NOT NULL,
                                               type                 VARCHAR(255)    NOT NULL,
                                               event_identifier     VARCHAR(255)    NOT NULL UNIQUE,
                                               meta_data            BYTEA,
                                               payload              BYTEA           NOT NULL,
                                               payload_revision     VARCHAR(255),
                                               payload_type         VARCHAR(255)    NOT NULL,
                                               time_stamp           VARCHAR(255)    NOT NULL,

                                               PRIMARY KEY (aggregate_identifier, sequence_number)
);

-- -----------------------------------------------------------------------
-- Axon token store
-- Tracks the position of each event processor in the event stream.
-- Used by tracking processors; included for future use.
-- -----------------------------------------------------------------------
CREATE TABLE platform.token_entry (
                                      processor_name  VARCHAR(255)    NOT NULL,
                                      segment         INTEGER         NOT NULL,
                                      token           BYTEA,
                                      token_type      VARCHAR(255),
                                      timestamp       VARCHAR(255),
                                      owner           VARCHAR(255),

                                      PRIMARY KEY (processor_name, segment)
);

-- -----------------------------------------------------------------------
-- Axon saga tables
-- Included for future use when sagas are introduced (e.g. tenant
-- provisioning workflow spanning multiple aggregates).
-- -----------------------------------------------------------------------
CREATE TABLE platform.saga_entry (
                                     saga_id         VARCHAR(255)    NOT NULL,
                                     revision        VARCHAR(255),
                                     saga_type       VARCHAR(255),
                                     serialized_saga BYTEA,

                                     PRIMARY KEY (saga_id)
);

CREATE TABLE platform.association_value_entry (
                                                  id              BIGSERIAL       NOT NULL,
                                                  association_key VARCHAR(255)    NOT NULL,
                                                  association_value VARCHAR(255),
                                                  saga_id         VARCHAR(255),
                                                  saga_type       VARCHAR(255),

                                                  PRIMARY KEY (id)
);

CREATE INDEX idx_association_value_entry_saga
    ON platform.association_value_entry (saga_id, saga_type);

CREATE INDEX idx_association_value_entry_key
    ON platform.association_value_entry (association_key, association_value, saga_type);

-- -----------------------------------------------------------------------
-- Tenant view — read model
--
-- Written exclusively by TenantProjection event handlers.
-- Never written by the command side.
-- -----------------------------------------------------------------------
CREATE TABLE platform.tenant_view (
                                      id           UUID                   PRIMARY KEY,
                                      tenant_id    VARCHAR(32)            NOT NULL,
                                      display_name VARCHAR(255)           NOT NULL,
                                      status       platform.tenant_status NOT NULL DEFAULT 'PROVISIONING',
                                      db_name      VARCHAR(63)            NULL,
                                      created_at   TIMESTAMPTZ            NOT NULL,
                                      updated_at   TIMESTAMPTZ            NOT NULL
);

CREATE UNIQUE INDEX idx_tenant_view_tenant_id
    ON platform.tenant_view (tenant_id);

CREATE INDEX idx_tenant_view_status
    ON platform.tenant_view (status)
    WHERE status != 'ACTIVE';

-- -----------------------------------------------------------------------
-- Privileges
-- Grant the application role access to all new tables.
-- The trigger function set_updated_at already exists from V1.
-- -----------------------------------------------------------------------
GRANT SELECT, INSERT, UPDATE, DELETE
    ON platform.domain_event_entry      TO invento_platform;
GRANT SELECT, INSERT, UPDATE, DELETE
    ON platform.snapshot_event_entry    TO invento_platform;
GRANT SELECT, INSERT, UPDATE, DELETE
    ON platform.token_entry             TO invento_platform;
GRANT SELECT, INSERT, UPDATE, DELETE
    ON platform.saga_entry              TO invento_platform;
GRANT SELECT, INSERT, UPDATE, DELETE
    ON platform.association_value_entry TO invento_platform;
GRANT SELECT, INSERT, UPDATE, DELETE
    ON platform.tenant_view             TO invento_platform;

-- Sequences created by BIGSERIAL need explicit grants.
GRANT USAGE, SELECT
    ON SEQUENCE platform.domain_event_entry_global_index_seq    TO invento_platform;
GRANT USAGE, SELECT
    ON SEQUENCE platform.association_value_entry_id_seq         TO invento_platform;

-- updated_at trigger on tenant_view reuses the function from V1.
CREATE TRIGGER trg_tenant_view_updated_at
    BEFORE UPDATE ON platform.tenant_view
    FOR EACH ROW
    EXECUTE FUNCTION platform.set_updated_at();