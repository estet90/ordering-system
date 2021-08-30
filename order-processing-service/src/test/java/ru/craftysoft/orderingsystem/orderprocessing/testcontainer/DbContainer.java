package ru.tsc.crm.organizationalstructure.cache.service.testcontainer;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.tsc.crm.organizationalstructure.cache.service.util.DbHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;


public enum T1CrmDbContainer {
    INSTANCE;

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres");

    public String getUrl() {
        if (isStarted.get()) {
            return postgres.getJdbcUrl();
        }
        return "jdbc:postgresql://localhost:8888/databaseName";
    }

    public String getUsername() {
        return postgres.getUsername();
    }

    public String getPassword() {
        return postgres.getPassword();
    }

    public void start() {
        if (!this.isStarted.compareAndSet(false, true)) {
            return;
        }
        postgres.start();
        init();
        executeMigrations();
    }

    public void stop() {
        if (!this.isStarted.compareAndSet(true, false)) {
            return;
        }
        postgres.stop();
    }

    public void reset() {
        DbHelper.executeQueries(
                this::getConnection,
                "DELETE FROM organizational_structure.organizations",
                "DELETE FROM organizational_structure.positions"
        );
    }

    private void init() {
        DbHelper.executeQueryFromClasspath(this::getConnection, "setup/1_init.sql");
        DbHelper.executeQueryFromClasspath(this::getConnection, "setup/2_drop_schema.sql");
        DbHelper.executeQueryFromClasspath(this::getConnection, "setup/3_create_schema.sql");
    }

    public void executeMigrations() {
        try (var connection = getConnection();
             var liquibase = new Liquibase(
                     "/db/migration/organizational-structure-changelog.xml",
                     new ClassLoaderResourceAccessor(),
                     DatabaseFactory.getInstance()
                             .findCorrectDatabaseImplementation(new JdbcConnection(connection)))
        ) {
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (SQLException | LiquibaseException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() {
        return DbHelper.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
