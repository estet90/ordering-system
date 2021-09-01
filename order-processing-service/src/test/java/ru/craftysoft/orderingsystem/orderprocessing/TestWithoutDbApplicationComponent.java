package ru.craftysoft.orderingsystem.orderprocessing;

import dagger.Component;
import ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven.DecreaseCustomerAmountOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.module.ExecutorModule;
import ru.craftysoft.orderingsystem.orderprocessing.module.GrpcClientModule;
import ru.craftysoft.orderingsystem.orderprocessing.module.TestPropertyModule;
import ru.craftysoft.orderingsystem.orderprocessing.module.TestRedisModule;

import javax.inject.Singleton;

@Component(modules = {
        TestPropertyModule.class,
        TestRedisModule.class,

        ExecutorModule.class,
        GrpcClientModule.class,
})
@Singleton
public interface TestWithoutDbApplicationComponent {

    void inject(DecreaseCustomerAmountOperationTest operationTest);

}
