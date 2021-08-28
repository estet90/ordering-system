package ru.craftysoft.orderingsystem.customer;

import dagger.Component;
import io.grpc.Server;
import ru.craftysoft.orderingsystem.customer.module.DbModule;
import ru.craftysoft.orderingsystem.customer.module.ExecutorModule;
import ru.craftysoft.orderingsystem.customer.module.ServerModule;
import ru.craftysoft.orderingsystem.util.properties.PropertyModule;

import javax.inject.Singleton;

@Component(modules = {
        PropertyModule.class,

        DbModule.class,
        ServerModule.class,
        ExecutorModule.class,
})
@Singleton
public interface ApplicationComponent {

    Server grpcServer();

}
