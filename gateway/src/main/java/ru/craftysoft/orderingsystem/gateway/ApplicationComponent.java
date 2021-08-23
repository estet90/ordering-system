package ru.craftysoft.orderingsystem.gateway;

import dagger.Component;
import io.vertx.core.Verticle;
import ru.craftysoft.orderingsystem.gateway.module.GrpcClientModule;
import ru.craftysoft.orderingsystem.gateway.module.ServerModule;
import ru.craftysoft.orderingsystem.util.jackson.JacksonModule;
import ru.craftysoft.orderingsystem.util.properties.PropertyModule;

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

}
