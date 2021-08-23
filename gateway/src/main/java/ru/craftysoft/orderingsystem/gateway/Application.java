package ru.craftysoft.orderingsystem.gateway;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {

    public static void main(String[] args) {
        var component = DaggerApplicationComponent.builder().build();
        var verticle = component.mainVerticle();
        Vertx.vertx().deployVerticle(verticle).onComplete(new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                if (event.succeeded()) {
                    log.info("Приложение запущено");
                } else {
                    log.error("Ошибка при запуске приложения", event.cause());
                }
            }
        });
    }

}
