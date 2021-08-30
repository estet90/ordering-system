package ru.craftysoft.orderingsystem.orderprocessing.logic;

import dagger.Component;
import ru.craftysoft.orderingsystem.orderprocessing.module.*;

@Component(modules = {
        TestPropertyModule.class,
        TestDbModule.class,

        ExecutorModule.class,
        GrpcClientModule.class,
})
public interface TestApplicationComponent {
}
