package ru.craftysoft.orderingsystem.gateway;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory;

@Slf4j
public class Application {

    public static void main(String[] args) {
        new ExceptionFactory("000");
        var component = DaggerApplicationComponent.builder().build();
        var verticle = component.mainVerticle();
        component.vertx().deployVerticle(verticle).onComplete(event -> {
            if (event.succeeded()) {
                log.info("Приложение запущено");
            } else {
                log.error("Ошибка при запуске приложения", event.cause());
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Приложение останавливается");
            component.customerServiceManagedChannel().shutdownNow();
            component.orderServiceManagedChannel().shutdownNow();
            component.userServiceManagedChannel().shutdownNow();
            component.executorServiceManagedChannel().shutdownNow();
            Vertx.vertx().close(event -> {
                if (event.succeeded()) {
                    log.info("Приложение остановлено");
                } else {
                    log.error("Ошибка при остановке приложения", event.cause());
                }
            });
        }));
    }

}
