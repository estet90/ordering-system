package ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.orderprocessing.proto.ReserveOrderRequest;
import ru.craftysoft.orderingsystem.orderprocessing.service.dao.OrderDaoAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * заказ снова резервируется. последняя операция при откате
 */
@Singleton
@Slf4j
public class ReserveOrderOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final OrderDaoAdapter orderDaoAdapter;

    @Inject
    public ReserveOrderOperation(RedisClientAdapter redisClientAdapter, OrderDaoAdapter orderDaoAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public void process() {
        redisClientAdapter.listenReserveOrderRequestMessages()
                .thenAccept(requests -> {
                    if (!requests.isEmpty()) {
                        log.info("ReserveOrderOperation.process.in size={}", requests.size());
                        requests.forEach(this::processMessage);
                        log.info("ReserveOrderOperation.process.out");
                    }
                });
    }

    private void processMessage(ReserveOrderRequest request) {
        try {
            orderDaoAdapter.reserveOrder(request);
        } catch (Exception e) {
            redisClientAdapter.retryReserveOrderRequestMessage(request, e)
                    .whenComplete((ignored, retryThrowable) -> {
                        if (retryThrowable != null) {
                            log.error("ReserveOrderOperation.processMessage.retry.thrown", retryThrowable);
                        } else {
                            log.error("ReserveOrderOperation.processMessage.retry.out");
                        }
                    });
        }
    }

}
