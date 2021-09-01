package ru.craftysoft.orderingsystem.orderprocessing;

import dagger.Component;
import ru.craftysoft.orderingsystem.orderprocessing.logic.ExtractOrderOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven.CompleteOrderOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.module.*;

import javax.inject.Singleton;

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

    void inject(CompleteOrderOperationTest operationTest);

}
