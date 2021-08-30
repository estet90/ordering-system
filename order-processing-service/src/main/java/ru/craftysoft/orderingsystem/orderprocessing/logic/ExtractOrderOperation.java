package ru.craftysoft.orderingsystem.orderprocessing.logic;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.service.dao.OrderDaoAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.EXTRACT_ORDER;
import static ru.craftysoft.orderingsystem.util.error.logging.ExceptionLoggerHelper.logError;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

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
    private final String point = "ExtractOrderOperation.process";

    @Inject
    public ExtractOrderOperation(RedisClientAdapter redisClientAdapter, OrderDaoAdapter orderDaoAdapter) {
        this.redisClientAdapter = redisClientAdapter;
        this.orderDaoAdapter = orderDaoAdapter;
    }

    public CompletableFuture<Void> process() {
        MDC.setContextMap(Map.of(
                TRACE_ID, UuidUtils.generateDefaultUuid(),
                SPAN_ID, UuidUtils.generateDefaultUuid(),
                OPERATION_NAME, EXTRACT_ORDER.name()
        ));
        log.info("{}.in", point);
        try {
            var orders = orderDaoAdapter.processOrders();
            var futures = orders.stream().map(order -> redisClientAdapter.sendMessagesToDecreaseCustomerAmountStream(order)
                            .whenComplete(withMdc((unused, throwable) -> {
                                if (throwable != null) {
                                    logError(log, point + ".processOrder", throwable);
                                    try {
                                        orderDaoAdapter.reserveOrder(order);
                                    } catch (Exception e) {
                                        logError(log, point + ".reserveOrder", e);
                                    }
                                }
                            })))
                    .toArray(value -> new CompletableFuture[orders.size()]);
            log.info("{}.out", point);
            return CompletableFuture.allOf(futures);
        } catch (Exception e) {
            logError(log, point, e);
            throw new RuntimeException(e);
        } finally {
            MDC.clear();
        }
    }
}
