package ru.craftysoft.orderingsystem.orderprocessing;

import dagger.Component;
import ru.craftysoft.orderingsystem.orderprocessing.logic.ExtractOrderOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven.CompleteOrderOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven.DecreaseCustomerAmountOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.module.*;

import javax.inject.Singleton;

@Component(modules = {
        TestPropertyModule.class,
        TestRedisModule.class,
        TestDbNopModule.class,

        ExecutorModule.class,
        GrpcClientModule.class,
})
@Singleton
public interface TestWithoutDbApplicationComponent {

    void inject(DecreaseCustomerAmountOperationTest operationTest);

    void inject(ExtractOrderOperationTest operationTest);

    void inject(CompleteOrderOperationTest operationTest);

}
