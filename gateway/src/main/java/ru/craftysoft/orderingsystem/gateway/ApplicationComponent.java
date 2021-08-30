package ru.craftysoft.orderingsystem.gateway;

import dagger.Component;
import io.grpc.ManagedChannel;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import ru.craftysoft.orderingsystem.gateway.module.GrpcClientModule;
import ru.craftysoft.orderingsystem.gateway.module.ServerModule;
import ru.craftysoft.orderingsystem.util.jackson.JacksonModule;
import ru.craftysoft.orderingsystem.util.properties.PropertyModule;

import javax.inject.Named;
import javax.inject.Singleton;

@Component(modules = {
        ServerModule.class,
        GrpcClientModule.class,

        PropertyModule.class,
        JacksonModule.class,
})
@Singleton
public interface ApplicationComponent {

    Verticle mainVerticle();

    Vertx vertx();

    @Named("userServiceManagedChannel")
    ManagedChannel userServiceManagedChannel();

    @Named("orderServiceManagedChannel")
    ManagedChannel orderServiceManagedChannel();

    @Named("customerServiceManagedChannel")
    ManagedChannel customerServiceManagedChannel();

    @Named("executorServiceManagedChannel")
    ManagedChannel executorServiceManagedChannel();

}
