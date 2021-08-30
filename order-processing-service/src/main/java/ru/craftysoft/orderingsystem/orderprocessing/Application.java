package ru.craftysoft.orderingsystem.orderprocessing;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class Application {

    public static void main(String[] args) {
        new ExceptionFactory("005");
        var component = DaggerApplicationComponent.builder().build();
        component.redisConsumerGroupInitOperation().process();
        component.extractOrderExecutor()
                .scheduleWithFixedDelay(component.extractOrderOperation()::process, 0, 5, SECONDS);
        component.completeOrderExecutor()
                .scheduleWithFixedDelay(component.completeOrderOperation()::process, 0, 5, SECONDS);
        component.reserveOrderExecutor()
                .scheduleWithFixedDelay(component.reserveOrderOperation()::process, 0, 5, SECONDS);
        component.decreaseCustomerAmountExecutor()
                .scheduleWithFixedDelay(component.decreaseCustomerAmountOperation()::process, 0, 5, SECONDS);
        component.decreaseExecutorAmountExecutor()
                .scheduleWithFixedDelay(component.decreaseExecutorAmountOperation()::process, 0, 5, SECONDS);
        component.incrementCustomerAmountExecutor()
                .scheduleWithFixedDelay(component.incrementCustomerAmountOperation()::process, 0, 5, SECONDS);
        component.incrementExecutorAmountExecutor()
                .scheduleWithFixedDelay(component.incrementExecutorAmountOperation()::process, 0, 5, SECONDS);
        log.info("Приложение запущено");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            component.extractOrderExecutor().shutdownNow();
            component.completeOrderExecutor().shutdownNow();
            component.reserveOrderExecutor().shutdownNow();
            component.decreaseCustomerAmountExecutor().shutdownNow();
            component.decreaseExecutorAmountExecutor().shutdownNow();
            component.incrementCustomerAmountExecutor().shutdownNow();
            component.incrementExecutorAmountExecutor().shutdownNow();
            component.customerServiceManagedChannel().shutdownNow();
            component.executorServiceManagedChannel().shutdownNow();
            component.redisPool().close();
            log.info("Приложение остановлено");
        }));
    }

}
