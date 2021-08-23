package ru.craftysoft.orderingsystem.gateway.module;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import ru.craftysoft.orderingsystem.gateway.controller.OrderController;
import ru.craftysoft.orderingsystem.gateway.provider.SecurityRequestFilter;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Singleton;
import java.util.List;

@Module
@Slf4j
public class ServerModule {

    @Provides
    @Singleton
    static Verticle mainVerticle(SecurityRequestFilter securityRequestFilter,
                                 PropertyResolver propertyResolver,
                                 OrderController orderController) {
        return new AbstractVerticle() {
            @Override
            public void start() {
                var deployment = new VertxResteasyDeployment();
                deployment.setProviders(List.of(securityRequestFilter));
                deployment.start();
                var path = propertyResolver.getStringProperty("server.path");
                deployment.getRegistry().addSingletonResource(orderController, path);
                var port = propertyResolver.getIntProperty("server.port");
                vertx.createHttpServer()
                        .requestHandler(new VertxRequestHandler(vertx, deployment))
                        .listen(port, ar -> {
                            if (ar.succeeded()) {
                                log.info("Сервер запущен на порту {}", port);
                            } else {
                                log.error("Ошибка при старте сервера", ar.cause());
                            }
                        });
            }
        };
    }

}
