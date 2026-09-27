package com.chait.inventocoreservice.db.provisioning;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V1__create_databases extends BaseJavaMigration {

    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context context) throws Exception {
        final Connection connection = context.getConnection();

        createRoleIfAbsent(connection);
        createDatabaseIfAbsent(connection, "invento_platform", "invento_platform");
        createDatabaseIfAbsent(connection, "keycloak",         "invento_platform");
        createDatabaseIfAbsent(connection, "invento_acme",     "invento_platform");
        createDatabaseIfAbsent(connection, "invento_globex",   "invento_platform");
        grantPrivileges(connection);
    }

    private void createRoleIfAbsent(Connection connection) throws Exception {
        if (roleExists(connection, "invento_platform")) {
            log("Role invento_platform already exists — skipping.");
            return;
        }

        execute(connection, """
                CREATE ROLE invento_platform
                    WITH LOGIN
                         PASSWORD 'invento_platform'
                         NOSUPERUSER
                         NOCREATEDB
                         NOCREATEROLE
                         NOREPLICATION
                """);

        log("Role invento_platform created.");
    }

    private boolean roleExists(Connection connection, String roleName) throws Exception {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = '"
                             + roleName + "'")) {
            return rs.next();
        }
    }

    private void createDatabaseIfAbsent(Connection connection,
                                        String dbName,
                                        String owner) throws Exception {
        if (databaseExists(connection, dbName)) {
            log("Database " + dbName + " already exists — skipping.");
            return;
        }

        // Cannot use prepared statements for DDL in Postgres.
        execute(connection, """
                CREATE DATABASE %s
                    WITH OWNER      = %s
                         ENCODING   = 'UTF8'
                         LC_COLLATE = 'en_US.UTF-8'
                         LC_CTYPE   = 'en_US.UTF-8'
                         TEMPLATE   = template0
                """.formatted(dbName, owner));

        log("Database " + dbName + " created.");
    }

    private boolean databaseExists(Connection connection,
                                   String dbName) throws Exception {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT 1 FROM pg_database WHERE datname = '"
                             + dbName + "'")) {
            return rs.next();
        }
    }

    private void grantPrivileges(Connection connection) throws Exception {
        for (String db : new String[]{
                "invento_platform", "invento_acme", "invento_globex"}) {
            execute(connection,
                    "GRANT CONNECT ON DATABASE " + db + " TO invento_platform");
        }
        log("CONNECT privileges granted to invento_platform.");
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        }
    }

    private void log(String message) {
        System.out.println("[Provisioning] " + message);
    }
}
