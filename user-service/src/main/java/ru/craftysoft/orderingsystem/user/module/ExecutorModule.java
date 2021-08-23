package ru.craftysoft.orderingsystem.user.module;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dagger.Module;
import dagger.Provides;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Module
public class ExecutorModule {

    @Provides
    @Singleton
    @Named("dbExecutor")
    static Executor dbExecutor(PropertyResolver propertyResolver) {
        return Executors.newFixedThreadPool(
                propertyResolver.getIntProperty("db.pool.size"),
                new ThreadFactoryBuilder().setNameFormat("users-db-thread-%d").build()
        );
    }

}
