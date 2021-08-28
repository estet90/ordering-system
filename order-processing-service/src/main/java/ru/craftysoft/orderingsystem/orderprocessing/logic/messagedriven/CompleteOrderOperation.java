package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.orderprocessing.proto.CompleteOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.dao.OrderDaoAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * заказ помечается как выполненный
 * при возникновении ошибки пытаемся выполнить повторный вызов. после истечения всех попыток переходим к {@link DecreaseExecutorAmountOperation}
 */
@Singleton
@Slf4j
public class CompleteOrderOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final OrderDaoAdapter orderDaoAdapter;

    @Inject
    public CompleteOrderOperation(RedisClientAdapter redisClientAdapter, OrderDaoAdapter orderDaoAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public void process() {
        redisClientAdapter.listenCompleteOrderRequestMessages()
                .thenAccept(requests -> {
                    if (!requests.isEmpty()) {
                        log.info("CompleteOrderOperation.process.in size={}", requests.size());
                        requests.forEach(this::processMessage);
                        log.info("CompleteOrderOperation.process.out");
                    }
                });
    }

    private void processMessage(CompleteOrderRequest request) {
        try {
            orderDaoAdapter.completeOrder(request);
        } catch (Exception e) {
            redisClientAdapter.retryCompleteOrderRequestMessage(request, e)
                    .whenComplete((ignored, retryThrowable) -> {
                        if (retryThrowable != null) {
                            log.error("CompleteOrderOperation.processMessage.retry.thrown", retryThrowable);
                            redisClientAdapter.sendMessageToDecreaseExecutorAmountStream(request)
                                    .whenComplete((unused, throwable) -> {
                                        if (throwable != null) {
                                            log.error("CompleteOrderOperation.processMessage.rollback.thrown", throwable);
                                        }
                                    });
                        } else {
                            log.error("CompleteOrderOperation.processMessage.retry.out");
                        }
                    });
        }
    }
}
