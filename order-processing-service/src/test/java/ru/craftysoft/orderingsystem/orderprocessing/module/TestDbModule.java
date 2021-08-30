package ru.craftysoft.orderingsystem.orderprocessing.module;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.Module;
import dagger.Provides;
import ru.craftysoft.orderingsystem.orderprocessing.testcontainer.DbContainer;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Singleton;
import javax.sql.DataSource;

@Module
public class TestDbModule {

    @Provides
    @Singleton
    static DataSource dataSource(PropertyResolver propertyResolver) {
        var config = new HikariConfig();
        config.setJdbcUrl(DbContainer.INSTANCE.getUrl());
        config.setUsername(DbContainer.INSTANCE.getUsername());
        config.setPassword(DbContainer.INSTANCE.getPassword());
        config.setMaximumPoolSize(propertyResolver.getIntProperty("db.pool.size"));
        return new HikariDataSource(config);
    }

    @Provides
    @Singleton
    static DbHelper dbHelper(DataSource dataSource) {
        return new DbHelper(dataSource);
    }

}
