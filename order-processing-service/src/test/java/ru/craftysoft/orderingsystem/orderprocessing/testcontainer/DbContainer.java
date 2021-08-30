package ru.craftysoft.orderingsystem.orderprocessing.testcontainer;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.craftysoft.orderingsystem.orderprocessing.util.TestDbHelper;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public enum DbContainer {
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
        executeMigrations(Set.of("order-db", "executor-db", "customer-db"), "/db/migration/users-changelog.xml");
        executeMigrations(Set.of("user-db", "executor-db", "customer-db"), "/db/migration/orders-changelog.xml");
        executeMigrations(Set.of("order-db", "user-db", "customer-db"), "/db/migration/executors-changelog.xml");
        executeMigrations(Set.of("order-db", "executor-db", "user-db"), "/db/migration/customers-changelog.xml");
    }

    public void stop() {
        if (!this.isStarted.compareAndSet(true, false)) {
            return;
        }
        postgres.stop();
    }

    private void init() {
        TestDbHelper.executeQueryFromClasspath(this::getConnection, "setup/1_init.sql");
        TestDbHelper.executeQueryFromClasspath(this::getConnection, "setup/2_drop_schema.sql");
        TestDbHelper.executeQueryFromClasspath(this::getConnection, "setup/3_create_schema.sql");
    }

    @SneakyThrows
    private void executeMigrations(Set<String> excludedProjects, String changelogFile) {
        var classPath = System.getProperty("java.class.path").split(File.pathSeparator);
        var urls = new ArrayList<URL>();
        classPathLoop:
        for (var resource : classPath) {
            for (var excludedProject : excludedProjects) {
                if (resource.contains(excludedProject)) {
                    continue classPathLoop;
                }
            }
            urls.add(new File(resource).toURI().toURL());
        }
        var platformClassLoader = ClassLoader.getPlatformClassLoader();
        var topClassLoader = platformClassLoader.getParent() != null
                ? platformClassLoader.getParent()
                : platformClassLoader;

        var classLoader = new URLClassLoader(urls.toArray(URL[]::new), topClassLoader);
        try (var connection = getConnection();
             var liquibase = new Liquibase(
                     changelogFile,
                     new ClassLoaderResourceAccessor(classLoader),
                     DatabaseFactory.getInstance()
                             .findCorrectDatabaseImplementation(new JdbcConnection(connection)))

        ) {
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (SQLException | LiquibaseException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() {
        return TestDbHelper.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
