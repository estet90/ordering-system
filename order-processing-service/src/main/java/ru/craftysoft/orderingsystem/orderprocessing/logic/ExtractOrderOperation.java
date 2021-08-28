package ru.craftysoft.orderingsystem.orderprocessing.logic;

import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.orderingsystem.orderprocessing.service.dao.OrderDaoAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * из БД извлекаются зарезервированные заявки и передаются в работу.
 * в случае успеха переходим к {@link ru.craftysoft.orderingsystem.orderprocessing.logic.messagedriven.DecreaseCustomerAmountOperation}.
 * при возникновении ошибки с отправкой обновляем статус заявки на 'reserved'.
 */
@Singleton
@Slf4j
public class ExtractOrderOperation {

    private final RedisClientAdapter redisClientAdapter;
    private final OrderDaoAdapter orderDaoAdapter;

    @Inject
    public ExtractOrderOperation(RedisClientAdapter redisClientAdapter, OrderDaoAdapter orderDaoAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public void process() {
        log.info("ExtractOrderOperation.process.in");
        try {
            var orders = orderDaoAdapter.processOrders();
            orders.forEach(order -> redisClientAdapter.sendMessagesToDecreaseCustomerAmountStream(order)
                    .whenComplete((unused, throwable) -> {
                        if (throwable != null) {
                            log.error("ExtractOrderOperation.process.processOrder.thrown order={}", order, throwable);
                            try {
                                orderDaoAdapter.reserveOrder(order);
                            } catch (Exception e) {
                                log.error("ExtractOrderOperation.process.reserveOrder.thrown", e);
                            }
                        }
                    }));
            log.info("ExtractOrderOperation.process.out");
        } catch (Exception e) {
            log.error("ExtractOrderOperation.process.thrown", e);
        }
    }
}
