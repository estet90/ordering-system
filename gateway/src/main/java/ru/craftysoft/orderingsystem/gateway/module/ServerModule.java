package ru.craftysoft.orderingsystem.gateway.module;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import ru.craftysoft.orderingsystem.gateway.controller.OrderController;
import ru.craftysoft.orderingsystem.gateway.controller.SwaggerController;
import ru.craftysoft.orderingsystem.gateway.provider.ExceptionHandler;
import ru.craftysoft.orderingsystem.gateway.provider.RequestLoggingFilter;
import ru.craftysoft.orderingsystem.gateway.provider.ResponseLoggingFilter;
import ru.craftysoft.orderingsystem.gateway.provider.SecurityRequestFilter;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Module
@Slf4j
public class ServerModule {

    @Provides
    @Singleton
    static Vertx vertx(VertxOptions vertxOptions) {
        return Vertx.vertx(vertxOptions);
    }

    @Provides
    @Singleton
    static VertxOptions vertxOptions(Set<Consumer<VertxOptions>> customizers) {
        var options = new VertxOptions()
                .setPreferNativeTransport(true);
        customizers.forEach(customizer -> customizer.accept(options));
        return options;
    }

    @Provides
    @ElementsIntoSet
    static Set<Consumer<VertxOptions>> customizers() {
        return new HashSet<>();
    }

    @Provides
    @Singleton
    static Verticle mainVerticle(SecurityRequestFilter securityRequestFilter,
                                 RequestLoggingFilter requestLoggingFilter,
                                 ResponseLoggingFilter responseLoggingFilter,
                                 ExceptionHandler exceptionHandler,
                                 PropertyResolver propertyResolver,
                                 OrderController orderController,
                                 SwaggerController swaggerController) {
        return new AbstractVerticle() {
            @Override
            public void start() {
                var deployment = new VertxResteasyDeployment();
                deployment.setProviders(List.of(securityRequestFilter, requestLoggingFilter, responseLoggingFilter, exceptionHandler));
                deployment.start();
                var path = propertyResolver.getStringProperty("server.path");
                var registry = deployment.getRegistry();
                registry.addSingletonResource(orderController, path);
                registry.addSingletonResource(swaggerController, path);
                var port = propertyResolver.getIntProperty("server.port");
                vertx.createHttpServer()
                        .requestHandler(new VertxRequestHandler(vertx, deployment))
                        .listen(port, ar -> {
                            if (ar.succeeded()) {
                                log.info("???????????? ?????????????? ???? ?????????? {}", port);
                            } else {
                                log.error("???????????? ?????? ???????????? ??????????????", ar.cause());
                            }
                        });
            }
        };
    }

}
