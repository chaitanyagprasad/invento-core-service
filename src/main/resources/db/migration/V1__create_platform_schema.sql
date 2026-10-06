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
