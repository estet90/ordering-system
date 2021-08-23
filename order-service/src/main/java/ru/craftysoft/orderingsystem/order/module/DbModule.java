package ru.craftysoft.orderingsystem.order.module;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.Module;
import dagger.Provides;
import ru.craftysoft.orderingsystem.util.db.DbHelper;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Singleton;
import javax.sql.DataSource;

@Module
public class DbModule {

    @Provides
    @Singleton
    static DataSource dataSource(PropertyResolver propertyResolver) {
        var config = new HikariConfig();
        config.setJdbcUrl(propertyResolver.getStringProperty("db.url"));
        config.setUsername(propertyResolver.getStringProperty("db.username"));
        config.setPassword(propertyResolver.getStringProperty("db.password"));
        config.setMaximumPoolSize(propertyResolver.getIntProperty("db.pool.size"));
        return new HikariDataSource(config);
    }

    @Provides
    @Singleton
    static DbHelper dbHelper(DataSource dataSource) {
        return new DbHelper(dataSource);
    }

}
