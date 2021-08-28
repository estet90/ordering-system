package ru.craftysoft.orderingsystem.orderprocessing.module;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dagger.Module;
import dagger.Provides;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Module
public class ExecutorModule {

    @Provides
    @Singleton
    @Named("dbExecutor")
    static Executor dbExecutor(PropertyResolver propertyResolver) {
        return Executors.newFixedThreadPool(
                propertyResolver.getIntProperty("db.pool.size"),
                new ThreadFactoryBuilder().setNameFormat("orders-db-thread-%d").build()
        );
    }

    @Provides
    @Singleton
    @Named("extractOrderExecutor")
    static ScheduledExecutorService extractOrderExecutor() {
        return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("extract-order-thread-%d").build());
    }

    @Provides
    @Singleton
    @Named("incrementCustomerAmountExecutor")
    static ScheduledExecutorService incrementCustomerAmountExecutor() {
        return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("increment-customer-amount-thread-%d").build());
    }

    @Provides
    @Singleton
    @Named("decreaseCustomerAmountExecutor")
    static ScheduledExecutorService decreaseCustomerAmountExecutor() {
        return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("decrease-customer-amount-thread-%d").build());
    }

    @Provides
    @Singleton
    @Named("incrementExecutorAmountExecutor")
    static ScheduledExecutorService incrementExecutorAmountExecutor() {
        return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("increment-executor-amount-thread-%d").build());
    }

    @Provides
    @Singleton
    @Named("decreaseExecutorAmountExecutor")
    static ScheduledExecutorService decreaseExecutorAmountExecutor() {
        return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("decrease-executor-amount-thread-%d").build());
    }

    @Provides
    @Singleton
    @Named("reserveOrderExecutor")
    static ScheduledExecutorService reserveOrderExecutor() {
        return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("reserve-order-thread-%d").build());
    }

    @Provides
    @Singleton
    @Named("completeOrderExecutor")
    static ScheduledExecutorService completeOrderExecutor() {
        return Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("complete-order-thread-%d").build());
    }

}
