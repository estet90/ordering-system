package ru.craftysoft.orderingsystem.orderprocessing;

import dagger.Component;
import ru.craftysoft.orderingsystem.orderprocessing.logic.ExtractOrderOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven.DecreaseCustomerAmountOperationTest;
import ru.craftysoft.orderingsystem.orderprocessing.module.*;
import ru.craftysoft.orderingsystem.util.db.DbHelper;

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

}
