package ru.craftysoft.orderingsystem.gateway;

import com.google.type.Money;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Slf4j
public class Application {

    public static void main(String[] args) {
//        var money = Money.newBuilder()
//                .setUnits(100)
//                .setNanos(100_000_000)
//                .build();
//        var bigDecimal = (new BigDecimal(money.getUnits())
//                .add(new BigDecimal(money.getNanos()).divide(new BigDecimal(1000_000_000), MathContext.DECIMAL32)))
//                .setScale(2, RoundingMode.HALF_DOWN);
//        System.out.println(bigDecimal);
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
