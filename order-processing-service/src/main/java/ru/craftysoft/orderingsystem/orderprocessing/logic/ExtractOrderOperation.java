package ru.craftysoft.orderingsystem.orderprocessing.logic;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode;
import ru.craftysoft.orderingsystem.orderprocessing.service.dao.OrderDaoAdapter;
import ru.craftysoft.orderingsystem.orderprocessing.service.redis.RedisClientAdapter;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ru.craftysoft.orderingsystem.orderprocessing.error.operation.ModuleOperationCode.EXTRACT_ORDER;
import static ru.craftysoft.orderingsystem.util.error.exception.ExceptionFactory.mapException;
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
                            .toCompletableFuture()
                            .exceptionallyCompose(withMdc(throwable -> {
                                logError(log, point + ".processOrder", throwable);
                                return orderDaoAdapter.reserveOrder(order)
                                        .whenComplete(withMdc((v, reserveOrderThrowable) -> {
                                            if (reserveOrderThrowable != null) {
                                                logError(log, point + ".reserveOrder", reserveOrderThrowable);
                                            }
                                        }))
                                        .thenApply(withMdc(v -> "nop"));
                            })))
                    .toArray(value -> new CompletableFuture[orders.size()]);
            return CompletableFuture.allOf(futures)
                    .thenAccept(withMdc(v -> {
                        log.info("{}.out", point);
                    }));
        } catch (Exception e) {
            var baseException = mapException(e, ModuleOperationCode::resolve);
            logError(log, point, baseException);
            throw baseException;
        } finally {
            MDC.clear();
        }
    }
}
