-- -----------------------------------------------------------------------
-- V1 : Platform schema and tenant registry
--
-- Runs against invento_platform as invento_platform (application user).
-- The platform schema is the control plane namespace — all cross-tenant
-- system tables live here, never in public.
--
-- Per-tenant databases (invento_acme, invento_globex, ...) have their
-- own separate Flyway migration runs scoped to their own schema.
-- The db_name column in tenant_registry is the link between the two.
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- Schema
-- -----------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS platform;

-- Schema-level privileges for the application role.
-- CONNECT on the database was granted in the provisioning migration.
GRANT USAGE  ON SCHEMA platform TO invento_platform;
GRANT SELECT, INSERT, UPDATE, DELETE
      ON ALL TABLES IN SCHEMA platform TO invento_platform;

-- Ensure future tables created by Flyway migrations are also accessible.
ALTER DEFAULT PRIVILEGES IN SCHEMA platform
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO invento_platform;

-- -----------------------------------------------------------------------
-- Enum
-- -----------------------------------------------------------------------
CREATE TYPE platform.tenant_status AS ENUM (
    'PROVISIONING',  -- record created; database not yet available
    'ACTIVE',        -- fully operational; logins accepted by the BFF
    'DISABLED'       -- suspended; BFF rejects with 403
);

-- -----------------------------------------------------------------------
-- Table
-- -----------------------------------------------------------------------
CREATE TABLE platform.tenant_registry (
                                          id           UUID                   PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Externally visible slug used in JWT tenant_id claim and gRPC
    -- x-tenant-id metadata. Matches BFF TenantId pattern ^[a-z0-9-]{2,32}$.
                                          tenant_id    VARCHAR(32)            NOT NULL,

                                          display_name VARCHAR(255)           NOT NULL,

                                          status       platform.tenant_status NOT NULL DEFAULT 'PROVISIONING',

    -- Name of the tenant's dedicated Postgres database.
    -- NULL while status = PROVISIONING (database not yet created).
    -- Follows the naming convention invento_{tenant_id}.
    -- e.g. 'invento_acme'
                                          db_name      VARCHAR(63)            NULL,

                                          created_at   TIMESTAMPTZ            NOT NULL DEFAULT now(),
                                          updated_at   TIMESTAMPTZ            NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------
-- Constraints
-- -----------------------------------------------------------------------

-- Unique slug.
ALTER TABLE platform.tenant_registry
    ADD CONSTRAINT uq_tenant_registry_tenant_id
        UNIQUE (tenant_id);

-- Slug format must match the BFF's TenantId validation pattern.
ALTER TABLE platform.tenant_registry
    ADD CONSTRAINT chk_tenant_registry_tenant_id_format
        CHECK (tenant_id ~ '^[a-z0-9-]{2,32}$');

-- Display name must not be blank.
ALTER TABLE platform.tenant_registry
    ADD CONSTRAINT chk_tenant_registry_display_name_not_blank
        CHECK (length(trim(display_name)) > 0);

-- db_name must follow Postgres identifier rules when set:
-- starts with a letter, then lowercase alphanumeric or underscores,
-- max 63 characters (Postgres namedatalen limit).
ALTER TABLE platform.tenant_registry
    ADD CONSTRAINT chk_tenant_registry_db_name_format
        CHECK (db_name IS NULL OR db_name ~ '^[a-z][a-z0-9_]{1,62}$');

-- A tenant cannot become ACTIVE without a database to connect to.
-- Enforces the provisioning sequence at the DB level:
--   create db → set db_name → set status = ACTIVE.
ALTER TABLE platform.tenant_registry
    ADD CONSTRAINT chk_tenant_registry_active_requires_db
        CHECK (status != 'ACTIVE' OR db_name IS NOT NULL);

-- -----------------------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------------------

-- Primary BFF lookup path: resolve tenant slug on every authenticated
-- request. Must be fast — this is on the hot path for every API call.
CREATE UNIQUE INDEX idx_tenant_registry_tenant_id
    ON platform.tenant_registry (tenant_id);

-- Platform admin queries: find tenants by lifecycle status.
-- Partial index excludes ACTIVE rows since the vast majority of tenants
-- are ACTIVE at any given time, keeping the index small.
CREATE INDEX idx_tenant_registry_status
    ON platform.tenant_registry (status)
    WHERE status != 'ACTIVE';

-- -----------------------------------------------------------------------
-- updated_at trigger
-- Maintains updated_at automatically so the application layer never
-- needs to set it explicitly. Defined in the platform schema so future
-- tables can reuse the function with their own trigger declarations.
-- -----------------------------------------------------------------------
CREATE FUNCTION platform.set_updated_at()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_tenant_registry_updated_at
    BEFORE UPDATE ON platform.tenant_registry
    FOR EACH ROW
    EXECUTE FUNCTION platform.set_updated_at();

-- -----------------------------------------------------------------------
-- Seed data
-- Dev/test tenants only. Production tenants are provisioned via the
-- platform admin API in Phase 3. db_name follows the naming convention.
-- -----------------------------------------------------------------------
INSERT INTO platform.tenant_registry
(tenant_id, display_name, status, db_name)
VALUES
    ('acme',   'Acme Corporation', 'ACTIVE', 'invento_acme'),
    ('globex', 'Globex Inc',       'ACTIVE', 'invento_globex');