package ru.craftysoft.orderingsystem.orderprocessing;

import dagger.Component;
import io.grpc.ManagedChannel;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.BoundedAsyncPool;
import ru.craftysoft.orderingsystem.orderprocessing.logic.ExtractOrderOperation;
import ru.craftysoft.orderingsystem.orderprocessing.logic.ExtractOrderOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.logic.RedisConsumerGroupInitOperation;
import ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven.*;
import ru.craftysoft.orderingsystem.orderprocessing.module.*;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;

@Component(modules = {
        TestPropertyModule.class,
        TestDbModule.class,
        TestRedisModule.class,

        ExecutorModule.class,
        GrpcClientModule.class,
})
@Singleton
public interface TestApplicationComponent {

    void inject(ExtractOrderOperationTest operationTest);

    RedisClient redisClient();

    BoundedAsyncPool<StatefulRedisConnection<String, byte[]>> redisPool();

    @Named("extractOrderExecutor")
    ScheduledExecutorService extractOrderExecutor();

    @Named("incrementCustomerAmountExecutor")
    ScheduledExecutorService incrementCustomerAmountExecutor();

    @Named("decreaseCustomerAmountExecutor")
    ScheduledExecutorService decreaseCustomerAmountExecutor();

    @Named("incrementExecutorAmountExecutor")
    ScheduledExecutorService incrementExecutorAmountExecutor();

    @Named("decreaseExecutorAmountExecutor")
    ScheduledExecutorService decreaseExecutorAmountExecutor();

    @Named("reserveOrderExecutor")
    ScheduledExecutorService reserveOrderExecutor();

    @Named("completeOrderExecutor")
    ScheduledExecutorService completeOrderExecutor();

    RedisConsumerGroupInitOperation redisConsumerGroupInitOperation();

    ExtractOrderOperation extractOrderOperation();

    CompleteOrderOperation completeOrderOperation();

    DecreaseCustomerAmountOperation decreaseCustomerAmountOperation();

    DecreaseExecutorAmountOperation decreaseExecutorAmountOperation();

    IncrementCustomerAmountOperation incrementCustomerAmountOperation();

    IncrementExecutorAmountOperation incrementExecutorAmountOperation();

    ReserveOrderOperation reserveOrderOperation();

    @Named("customerServiceManagedChannel")
    ManagedChannel customerServiceManagedChannel();

    @Named("executorServiceManagedChannel")
    ManagedChannel executorServiceManagedChannel();

}
